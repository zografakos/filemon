/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.FortUpdater;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.StaticObjects;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.zone.type.L2FortZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Fort
{
	protected static final Logger _log = Logger.getLogger(Fort.class.getName());
	
	// =========================================================
	// Data Field
	private int _fortId = 0;
	private List<L2DoorInstance> _doors = new FastList<L2DoorInstance>();
	private L2StaticObjectInstance _flagPole = null;
	private List<String> _doorDefault = new FastList<String>();
	private List<String> _flagPoleStats = new FastList<String>();
	private String _name = "";
	private FortManager	_spawnManager;
	private FortSiege _siege = null;
	private Calendar _siegeDate;
	private Calendar _lastOwnedTime;
	private L2FortZone _zone;
	private L2Clan _fortOwner = null;
	private int _fortType = 0;
	private int _state = 0;
	private int _castleId = 0;
	private FastMap<Integer, FortFunction> _function;
	
	/** Fortress Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public class FortFunction
	{
		private int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private long _rate;
		private long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public FortFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLvl()
		{
			return _lvl;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (getOwnerClan() == null)
				return;
			long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), _endDate - currentTime);
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), 0);
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			public void run()
			{
				try
				{
					if (getOwnerClan() == null)
						return;
					if (getOwnerClan().getWarehouse().getAdena() >= _fee || !_cwh)
					{
						int fee = _fee;
						boolean newfc = true;
						if (getEndTime() == 0 || getEndTime() == -1)
						{
							if (getEndTime() == -1)
							{
								newfc = false;
								fee = _tempFee;
							}
						}
						else
							newfc = false;
						setEndTime(System.currentTimeMillis() + getRate());
						dbSave(newfc);
						if (_cwh)
						{
							getOwnerClan().getWarehouse().destroyItemByItemId("CS_function_fee", 57, fee, null, null);
							if (Config.DEBUG)
								_log.warning("deducted " + fee + " adena from " + getName() + " owner's cwh for function id : " + getType());
						}
						ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(true), getRate());
					}
					else
						removeFunction(getType());
				}
				catch (Throwable t)
				{
				}
			}
		}
		
		public void dbSave(boolean newFunction)
		{
			java.sql.Connection con = null;
			try
			{
				PreparedStatement statement;
				
				con = L2DatabaseFactory.getInstance().getConnection();
				if (newFunction)
				{
					statement = con.prepareStatement("INSERT INTO fort_functions (fort_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
					statement.setInt(1, getFortId());
					statement.setInt(2, getType());
					statement.setInt(3, getLvl());
					statement.setInt(4, getLease());
					statement.setLong(5, getRate());
					statement.setLong(6, getEndTime());
				}
				else
				{
					statement = con.prepareStatement("UPDATE fort_functions SET lvl=?, lease=?, endTime=? WHERE fort_id=? AND type=?");
					statement.setInt(1, getLvl());
					statement.setInt(2, getLease());
					statement.setLong(3, getEndTime());
					statement.setInt(4, getFortId());
					statement.setInt(5, getType());
				}
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Exception: Fort.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	// =========================================================
	// Constructor
	public Fort(int fortId)
	{
		_fortId = fortId;
		load();
		loadDoor();
		loadFlagPoles();
		_function = new FastMap<Integer, FortFunction>();
		if (getOwnerClan() != null)
		{
			setVisibleFlag(true);
			loadFunctions();
		}
		_spawnManager = new FortManager(this);
	}
		
	/** Return function with id */
	public FortFunction getFunction(int type)
	{
		if (_function.get(type) != null)
			return _function.get(type);
		return null;
	}
	
	public class ScheduleSpecialEnvoysDeSpawn implements Runnable
	{
		private Fort _fortInst;
		
		public ScheduleSpecialEnvoysDeSpawn(Fort pFort)
		{
			_fortInst = pFort;
		}
		
		public void run()
		{
			try
			{
				// if state not decided, change state to indenpendent
				if (_fortInst.getFortState() == 0)
					_fortInst.setFortState(1,0);
				_fortInst.getSpawnManager().despawnSpecialEnvoys();
			}
			catch (Exception e)
			{
				_log.warning("Exception: ScheduleSpecialEnvoysSpawn() for Fort: "+_fortInst.getName()+" " + e.getMessage());
				e.printStackTrace();
			}
		}
	}	
	// =========================================================
	// Method - Public
	
	public void EndOfSiege(L2Clan clan)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new endFortressSiege(this, clan), 1000);
	}
	
	public void Engrave(L2Clan clan)
	{
		setOwner(clan, true);
	}
	
	/**
	 * Move non clan members off fort area and to nearest town.<BR><BR>
	 */
	public void banishForeigners()
	{
		_zone.banishForeigners(getOwnerClan());
	}
	
	/**
	 * Return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return _zone.isInsideZone(x, y, z);
	}
	
	/**
	 * Sets this forts zone
	 * @param zone
	 */
	public void setZone(L2FortZone zone)
	{
		_zone = zone;
	}
	
	public L2FortZone getZone()
	{
		return _zone;
	}
	
	/**
	 * Get the objects distance to this fort
	 * @param object
	 * @return
	 */
	public double getDistance(L2Object obj)
	{
		return _zone.getDistanceToZone(obj);
	}
	
	public void closeDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, false);
	}
	
	public void openDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, true);
	}
	
	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if (activeChar.getClan() != getOwnerClan())
			return;
		
		L2DoorInstance door = getDoor(doorId);
		if (door != null)
		{
			if (open)
				door.openMe();
			else
				door.closeMe();
		}
	}
	
	// This method is used to begin removing all fort upgrades
	public void removeUpgrade()
	{
		removeDoorUpgrade();
	}
	
	/**
	 * This method will set owner for Fort
	 * @param clan
	 * @param updateClanPoints
	 */
	public boolean setOwner(L2Clan clan, boolean updateClansReputation)
	{
		if (updateClansReputation)
		{
			// update reputation first
			updateClansReputation(clan);
		}
		// Remove old owner
		if (getOwnerClan() != null && (clan != null && clan != getOwnerClan()))
		{
			removeOwner(true);
		}
		setFortState(0,0); // initialize fort state
		//	if clan already have castle, don't store him in fortress
		if (clan.getHasCastle() > 0)
		{
			getSiege().announceToPlayer(new SystemMessage(SystemMessageId.NPCS_RECAPTURED_FORTRESS),0,false);
			return false;
		}
		else
		{
			getSpawnManager().spawnSpecialEnvoys();
			ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleSpecialEnvoysDeSpawn(this), 1*60*60*1000); // Prepare 1hr task for special envoys despawn
			// if clan have already fortress, remove it
			if (clan.getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(clan).removeOwner(true);

			setOwnerClan(clan);
			updateOwnerInDB(); // Update in database

			if (getSiege().getIsInProgress()) // If siege in progress
				getSiege().endSiege();
			return true;
		}
	}
	
	public void removeOwner(boolean updateDB)
	{
		L2Clan clan = getOwnerClan();
		if (clan != null)
		{
			clan.setHasFort(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			setOwnerClan(null);
			if (updateDB)
				updateOwnerInDB();

		}
	}
	/**
	 * Show or hide flag inside flagpole<BR><BR>
	 */
	public void setVisibleFlag(boolean val)
	{
			L2StaticObjectInstance flagPole = getFlagPole();
			if (flagPole != null)
				flagPole.setMeshIndex(val ? 1:0);
	}
	/**
	 * Respawn all doors on fort grounds<BR><BR>
	 */
	public void resetDoors()
	{
		for (int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if (door.getOpen())
				door.closeMe();
			if (door.getCurrentHp() <= 0)
				door.doRevive();
			if (door.getCurrentHp() < door.getMaxHp())
				door.setCurrentHp(door.getMaxHp());
		}
		loadDoorUpgrade(); // Check for any upgrade the doors may have
	}
	
	// This method upgrade door
	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
		L2DoorInstance door = getDoor(doorId);
		if (door == null)
			return;
		
		if (door != null && door.getDoorId() == doorId)
		{
			door.setCurrentHp(door.getMaxHp() + hp);
			
			saveDoorUpgrade(doorId, hp, pDef, mDef);
			return;
		}
	}
	
	// =========================================================
	// Method - Private
	// This method loads fort
	private void load()
	{
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from fort where id = ?");
			statement.setInt(1, getFortId());
			rs = statement.executeQuery();
			int ownerId = 0;
			
			while (rs.next())
			{
				_name = rs.getString("name");
				
				_siegeDate = Calendar.getInstance();
				_lastOwnedTime = Calendar.getInstance();
				_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
				_lastOwnedTime.setTimeInMillis(rs.getLong("lastOwnedTime"));
				ownerId = rs.getInt("owner");
				_fortType = rs.getInt("fortType");
				_state = rs.getInt("state");
				_castleId = rs.getInt("castleId");
			}
			
			rs.close();
			statement.close();
			
			if (ownerId > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(ownerId); // Try to find clan instance
				clan.setHasFort(getFortId());
				setOwnerClan(clan);
				ThreadPoolManager.getInstance().scheduleGeneral(new FortUpdater(clan, 1), 3600000); // Schedule owner tasks to start running
			}
			else
				setOwnerClan(null);
			
		}
		catch (Exception e)
		{
			_log.warning("Exception: loadFortData(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}
	
	/** Load All Functions */
	private void loadFunctions()
	{
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from fort_functions where fort_id = ?");
			statement.setInt(1, getFortId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_function.put(rs.getInt("type"), new FortFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Fort.loadFunctions(): " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/** Remove function In List and in DB */
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM fort_functions WHERE fort_id=? AND type=?");
			statement.setInt(1, getFortId());
			statement.setInt(2, functionType);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Fort.removeFunctions(int functionType): " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public boolean updateFunctions(L2PcInstance player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
			return false;
		if (Config.DEBUG)
			_log.warning("Called Fort.updateFunctions(int type, int lvl, int lease, long rate, boolean addNew) Owner : " + getOwnerClan());
		if (lease > 0)
			if (!player.destroyItemByItemId("Consume", 57, lease, null, true))
				return false;
		if (addNew)
		{
			_function.put(type, new FortFunction(type, lvl, lease, 0, rate, 0, false));
		}
		else
		{
			if (lvl == 0 && lease == 0)
				removeFunction(type);
			else
			{
				int diffLease = lease - _function.get(type).getLease();
				if (Config.DEBUG)
					_log.warning("Called Fort.updateFunctions diffLease : " + diffLease);
				if (diffLease > 0)
				{
					_function.remove(type);
					_function.put(type, new FortFunction(type, lvl, lease, 0, rate, -1, false));
				}
				else
				{
					_function.get(type).setLease(lease);
					_function.get(type).setLvl(lvl);
					_function.get(type).dbSave(false);
				}
			}
		}
		return true;
	}
	
	// This method loads fort door data from database
	private void loadDoor()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * from fort_staticobjects where fortId = ? and objectType = ?");
			statement.setInt(1, getFortId());
			statement.setInt(2, 0);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				// Create list of the door default for use when respawning dead doors
				_doorDefault.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";" + rs.getInt("range_xmin") + ";" + rs.getInt("range_ymin") + ";"
						+ rs.getInt("range_zmin") + ";" + rs.getInt("range_xmax") + ";" + rs.getInt("range_ymax") + ";" + rs.getInt("range_zmax") + ";" + rs.getInt("hp") + ";" + rs.getInt("pDef") + ";" + rs.getInt("mDef")+ ";" + rs.getBoolean("openType")+ ";" + rs.getBoolean("commanderDoor"));
				L2DoorInstance door;
				_doors.add(door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1)));
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				DoorTable.getInstance().putDoor(door);
			}
			
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: loadFortDoor(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}
	
	// This method loads fort flagpoles data from database
	private void loadFlagPoles()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * from fort_staticobjects where fortId = ? and objectType = ?");
			statement.setInt(1, getFortId());
			statement.setInt(2, 1);
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				//Create list of the door default for use when respawning dead doors
				_flagPoleStats.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";3;none;0;0");
				_flagPole = StaticObjects.parse(_flagPoleStats.get(_flagPoleStats.size() - 1));
				StaticObjects.getInstance().putObject(_flagPole);
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: loadFlagPoles(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	// This method loads fort door upgrade data from database
	private void loadDoorUpgrade()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * from fort_doorupgrade where doorId in (Select Id from fort_staticobjects where fortId = ? and objectType = ?)");
			statement.setInt(1, getFortId());
			statement.setInt(2, 0);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: loadFortDoorUpgrade(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}
	
	private void removeDoorUpgrade()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("delete from fort_doorupgrade where doorId in (select id from fort_staticobjects where fortId=? and objectType = ?)");
			statement.setInt(1, getFortId());
			statement.setInt(2, 0);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: removeDoorUpgrade(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}
	
	private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO fort_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
			statement.setInt(1, doorId);
			statement.setInt(2, hp);
			statement.setInt(3, pDef);
			statement.setInt(4, mDef);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}
	
	private void updateOwnerInDB()
	{
		L2Clan clan = getOwnerClan();
		int clanId = 0;
		if (clan != null)
		{
			clanId = clan.getClanId();
			_lastOwnedTime.setTimeInMillis(System.currentTimeMillis());
		}
		else
			_lastOwnedTime.setTimeInMillis(0);

		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("UPDATE fort SET owner=?,lastOwnedTime=?,state=?,castleId=? where id = ?");
			statement.setInt(1, clanId);
			statement.setLong(2, _lastOwnedTime.getTimeInMillis());
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.setInt(5, getFortId());
			statement.execute();
			statement.close();

			// ============================================================================
			// Announce to clan memebers
			if (clan != null)
			{
				clan.setHasFort(getFortId()); // Set has fort flag for new owner
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_VICTORIOUS_IN_THE_FORTRESS_BATTLE_OF_S2);
				sm.addString(clan.getName());
				sm.addFortId(getFortId());
				Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
				for (L2PcInstance player : pls)
				{
						player.sendPacket(sm);
				}
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				ThreadPoolManager.getInstance().scheduleGeneral(new FortUpdater(clan, 1), 3600000); // Schedule owner tasks to start running
			}
		}
		catch (Exception e)
		{
			_log.warning("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	public final int getFortId()
	{
		return _fortId;
	}
	
	public final L2Clan getOwnerClan()
	{
		return _fortOwner;
	}

	public final void setOwnerClan(L2Clan clan)
	{
		setVisibleFlag(clan != null ? true : false);
		_fortOwner = clan;
	}

	public final L2DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
			return null;
		
		for (L2DoorInstance door: getDoors())
		{
			if (door.getDoorId() == doorId)
				return door;
		}
		return null;
	}
	
	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	public final L2StaticObjectInstance getFlagPole()
	{
		return _flagPole;
	}

	public final FortSiege getSiege()
	{
		if (_siege == null)
			_siege = new FortSiege(new Fort[]
			{
				this
			});
		return _siege;
	}
	
	public final Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	public final void setSiegeDate(Calendar siegeDate)
	{
		_siegeDate = siegeDate;
	}
	
	public final int getOwnedTime()
	{
		if (_lastOwnedTime.getTimeInMillis() == 0)
			return 0;
		
		return (int) ((System.currentTimeMillis() - _lastOwnedTime.getTimeInMillis()) / 1000);
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public void updateClansReputation(L2Clan owner)
	{
		if (owner != null)
		{
			owner.setReputationScore(owner.getReputationScore() + 200, true);
			owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
		}
	}
	private class endFortressSiege implements Runnable
	{
		private Fort _f;
		private L2Clan _clan;
		
		public endFortressSiege(Fort f, L2Clan clan)
		{
			_f = f;
			_clan = clan;
		}
		
		public void run()
		{
			try
			{
				_f.Engrave(_clan);
			}
			catch (Exception e)
			{
            	_log.warning("Exception in endFortressSiege "+e.getMessage());
            	e.printStackTrace();
			}
		}
		
	}

	/**
	 * @return Returns state of fortress.<BR><BR>
	 * 0 - not decided yet<BR>
	 * 1 - independent<BR>
	 * 2 - contracted with castle<BR>
	 */
	public final int getFortState()
	{
		return _state;
	}

	/**
	 * @param State.<BR>
	 * 0 - not decided yet<BR>
	 * 1 - independent<BR>
	 * 2 - contracted with castle<BR><BR>
	 * @param CastleId.<BR>
	 * set Castle Id for contracted fort
	 */
	public final void setFortState(int state, int castleId)
	{
		_state = state;
		_castleId = castleId;
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("UPDATE fort SET state=?,castleId=? where id = ?");
			statement.setInt(1, getFortState());
			statement.setInt(2, getCastleId());
			statement.setInt(3, getFortId());
			statement.execute();
			statement.close();

		}
		catch (Exception e)
		{
			_log.warning("Exception: setFortState(int state, int castleId): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}
	/**
	 * @return Returns Castle Id of fortress contracted with castle.
	 */
	public final int getCastleId()
	{
		return _castleId;
	}

	/**
	 * @return Returns fortress type.<BR><BR>
	 * 0 - small (3 commanders) <BR>
	 * 1 - big (4 commanders + control room)
	 */
	public final int getFortType()
	{
		return _fortType;
	}

	public final int getCastleIdFromEnvoy(int npcId)
	{
		return getSpawnManager().getEnvoyCastle(npcId);
	}
	/**
	 * @return Returns amount of barracks.
	 */
	public final int getFortSize()
	{
		return getFortType() == 0 ? 3 : 5;
	}
	
	public FortManager getSpawnManager()
	{
		return _spawnManager;
	}
}