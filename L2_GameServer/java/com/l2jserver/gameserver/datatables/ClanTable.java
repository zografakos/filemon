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
package com.l2jserver.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.FortSiege;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.network.communityserver.writepackets.WorldInfo;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.5.2.5 $ $Date: 2005/03/27 15:29:18 $
 */
public class ClanTable
{
	private static Logger _log = Logger.getLogger(ClanTable.class.getName());
	
	private Map<Integer, L2Clan> _clans;
	
	public static ClanTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public L2Clan[] getClans()
	{
		return _clans.values().toArray(new L2Clan[_clans.size()]);
	}
	
	private ClanTable()
	{
		// forums has to be loaded before clan data, because of last forum id used should have also memo included
		if (Config.COMMUNITY_TYPE > 0)
			ForumsBBSManager.getInstance().initRoot();
		
		_clans = new FastMap<Integer, L2Clan>();
		L2Clan clan;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM clan_data");
			ResultSet result = statement.executeQuery();
			
			// Count the clans
			int clanCount = 0;
			
			while (result.next())
			{
				int clanId = result.getInt("clan_id");
				_clans.put(clanId, new L2Clan(clanId));
				clan = getClan(clanId);
				if (clan.getDissolvingExpiryTime() != 0)
					scheduleRemoveClan(clan.getClanId());
				clanCount++;
			}
			result.close();
			statement.close();
			
			_log.info("Restored " + clanCount + " clans from the database.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring ClanTable.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
		allianceCheck();
		restorewars();
	}
	
	/**
	 * @param clanId
	 * @return
	 */
	public L2Clan getClan(int clanId)
	{
		L2Clan clan = _clans.get(Integer.valueOf(clanId));
		
		return clan;
	}
	
	public L2Clan getClanByName(String clanName)
	{
		for (L2Clan clan : getClans())
		{
			if (clan.getName().equalsIgnoreCase(clanName))
			{
				return clan;
			}
			
		}
		
		return null;
	}
	
	/**
	 * Creates a new clan and store clan info to database
	 *
	 * @param player
	 * @return NULL if clan with same name already exists
	 */
	public L2Clan createClan(L2PcInstance player, String clanName)
	{
		if (null == player)
			return null;
		
		if (Config.DEBUG)
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested a clan creation.");
		
		if (10 > player.getLevel())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN));
			return null;
		}
		if (0 != player.getClanId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_CREATE_CLAN));
			return null;
		}
		if (System.currentTimeMillis() < player.getClanCreateExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN));
			return null;
		}
		if (!Util.isAlphaNumeric(clanName) || 2 > clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return null;
		}
		if (16 < clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return null;
		}
		
		if (null != getClanByName(clanName))
		{
			// clan name is already taken
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
			sm.addString(clanName);
			player.sendPacket(sm);
			sm = null;
			return null;
		}
		
		L2Clan clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName);
		L2ClanMember leader = new L2ClanMember(clan, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle(), player.getAppearance().getSex(), player.getRace().ordinal());
		clan.setLeader(leader);
		leader.setPlayerInstance(player);
		clan.store();
		player.setClan(clan);
		player.setPledgeClass(leader.calculatePledgeClass(player));
		player.setClanPrivileges(L2Clan.CP_ALL);
		
		if (Config.DEBUG)
			_log.fine("New clan created: " + clan.getClanId() + " " + clan.getName());
		
		_clans.put(Integer.valueOf(clan.getClanId()), clan);
		
		//should be update packet only
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new SystemMessage(SystemMessageId.CLAN_CREATED));
		// notify CB server that a new Clan is created
		CommunityServerThread.getInstance().sendPacket(new WorldInfo(null, clan, WorldInfo.TYPE_UPDATE_CLAN_DATA));
		return clan;
	}
	
	public synchronized void destroyClan(int clanId)
	{
		L2Clan clan = getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));
		int castleId = clan.getHasCastle();
		if (castleId == 0)
		{
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clan);
			}
		}
		int fortId = clan.getHasFort();
		if (fortId == 0)
		{
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clan);
			}
		}
		L2ClanMember leaderMember = clan.getLeader();
		if (leaderMember == null)
			clan.getWarehouse().destroyAllItems("ClanRemove", null, null);
		else
			clan.getWarehouse().destroyAllItems("ClanRemove", clan.getLeader().getPlayerInstance(), null);
		
		for (L2ClanMember member : clan.getMembers())
		{
			clan.removeClanMember(member.getObjectId(), 0);
		}
		
		_clans.remove(clanId);
		IdFactory.getInstance().releaseId(clanId);
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?");
			statement.setInt(1, clanId);
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_notices WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			if (castleId != 0)
			{
				statement = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
				statement.setInt(1, castleId);
				statement.execute();
				statement.close();
			}
			if (fortId != 0)
			{
				Fort fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					L2Clan owner = fort.getOwnerClan();
					if (clan == owner)
						fort.removeOwner(true);
				}
			}
			if (Config.DEBUG)
				_log.fine("clan removed in db: " + clanId);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan from DB.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public void scheduleRemoveClan(final int clanId)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				if (getClan(clanId) == null)
				{
					return;
				}
				if (getClan(clanId).getDissolvingExpiryTime() != 0)
				{
					destroyClan(clanId);
				}
			}
		}, Math.max(getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis(), 300000));
	}
	
	public boolean isAllyExists(String allyName)
	{
		for (L2Clan clan : getClans())
		{
			if (clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName))
			{
				return true;
			}
		}
		return false;
	}
	
	public void storeclanswars(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2) VALUES(?,?,?,?)");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error storing clan wars data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		//SystemMessage msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_BEGUN);
		//
		SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		//msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_BEGUN);
		//msg.addString(clan1.getName());
		//clan2.broadcastToOnlineMembers(msg);
		// clan1 declared clan war.
		msg = new SystemMessage(SystemMessageId.CLAN_S1_DECLARED_WAR);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
	}
	
	public void deleteclanswars(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		clan1.deleteEnemyClan(clan2);
		clan2.deleteAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		//for(L2ClanMember player: clan1.getMembers())
		//{
		//	if(player.getPlayerInstance()!=null)
		//			player.getPlayerInstance().setWantsPeace(0);
		//}
		//for(L2ClanMember player: clan2.getMembers())
		//{
		//	if(player.getPlayerInstance()!=null)
		//			player.getPlayerInstance().setWantsPeace(0);
		//}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.execute();
			//statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			//statement.setInt(1,clanId2);
			//statement.setInt(2,clanId1);
			//statement.execute();
			
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan wars data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		//SystemMessage msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_ENDED);
		SystemMessage msg = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		msg = new SystemMessage(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
		//msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_ENDED);
		//msg.addString(clan1.getName());
		//clan2.broadcastToOnlineMembers(msg);
	}
	
	public void checkSurrender(L2Clan clan1, L2Clan clan2)
	{
		int count = 0;
		for (L2ClanMember player : clan1.getMembers())
		{
			if (player != null && player.getPlayerInstance().getWantsPeace() == 1)
				count++;
		}
		if (count == clan1.getMembers().length - 1)
		{
			clan1.deleteEnemyClan(clan2);
			clan2.deleteEnemyClan(clan1);
			deleteclanswars(clan1.getClanId(), clan2.getClanId());
		}
	}
	
	private void restorewars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT clan1, clan2, wantspeace1, wantspeace2 FROM clan_wars");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				getClan(rset.getInt("clan1")).setEnemyClan(rset.getInt("clan2"));
				getClan(rset.getInt("clan2")).setAttackerClan(rset.getInt("clan1"));
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring clan wars data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Check for nonexistent alliances
	 */
	private void allianceCheck()
	{
		for (L2Clan clan : _clans.values())
		{
			int allyId = clan.getAllyId();
			if (allyId != 0 && clan.getClanId() != allyId)
			{
				if (!_clans.containsKey(allyId))
				{
					clan.setAllyId(0);
					clan.setAllyName(null);
					clan.changeAllyCrest(0, true);
					clan.updateClanInDB();
					_log.info(getClass().getSimpleName()+": Removed alliance from clan: "+clan);
				}
			}
		}
	}
	
	public void storeClanScore()
	{
		for (L2Clan clan : _clans.values())
			clan.updateClanScoreInDB();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ClanTable _instance = new ClanTable();
	}
}
