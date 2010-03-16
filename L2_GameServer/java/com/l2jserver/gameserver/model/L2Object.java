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
package com.l2jserver.gameserver.model;

import com.l2jserver.Config;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.knownlist.ObjectKnownList;
import com.l2jserver.gameserver.model.actor.poly.ObjectPoly;
import com.l2jserver.gameserver.model.actor.position.ObjectPosition;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.network.L2GameClient;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;


/**
 * Mother class of all objects in the world wich ones is it possible
 * to interact (PC, NPC, Item...)<BR><BR>
 *
 * L2Object :<BR><BR>
 * <li>L2Character</li>
 * <li>L2ItemInstance</li>
 * <li>L2Potion</li>
 *
 */

public abstract class L2Object
{
    // =========================================================
    // Data Field
    private boolean _isVisible;                 // Object visibility
    private ObjectKnownList _knownList;
    private String _name;
    private int _objectId;                      // Object identifier
    private ObjectPoly _poly;
    private ObjectPosition _position;
	private int _instanceId = 0;

    private InstanceType _instanceType = null; 

    // =========================================================
    // Constructor
    public L2Object(int objectId)
    {
    	setInstanceType(InstanceType.L2Object);
    	_objectId = objectId;
        initKnownList();
        initPosition();
    }

    public static enum InstanceType
    {
    	L2Object(),
    	L2Character(L2Object),
    	L2Npc(L2Object, L2Character),
    	L2Playable(L2Object, L2Character, L2Npc),
    	L2Summon(L2Object, L2Character, L2Playable),
    	L2Decoy(L2Object, L2Character),
    	L2Trap(L2Object, L2Character),
    	L2PcInstance(L2Object, L2Character, L2Playable),
    	L2NpcInstance(L2Object, L2Character, L2Npc),
    	L2MerchantInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2StaticObjectInstance(L2Object, L2Character),
    	L2DoorInstance(L2Object, L2Character),
    	L2EffectPointInstance(L2Object, L2Character, L2Npc),
    	// Summons, Pets, Decoys and Traps
    	L2SummonInstance(L2Object, L2Character, L2Playable, L2Summon),
    	L2SiegeSummonInstance(L2Object, L2Character, L2Playable, L2Summon, L2SummonInstance),
    	L2MerchantSummonInstance(L2Object, L2Character, L2Playable, L2Summon, L2SummonInstance),
    	L2PetInstance(L2Object, L2Character, L2Playable, L2Summon),
    	L2BabyPetInstance(L2Object, L2Character, L2Playable, L2Summon, L2PetInstance),
    	L2DecoyInstance(L2Object, L2Character, L2Decoy),
    	L2TrapInstance(L2Object, L2Character, L2Trap),
    	// Attackable
    	L2Attackable(L2Object, L2Character, L2Npc),
    	L2GuardInstance(L2Object, L2Character, L2Npc, L2Attackable),
    	L2MonsterInstance(L2Object, L2Character, L2Npc, L2Attackable),
    	L2ChestInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2ControllableMobInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2FeedableBeastInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2TamedBeastInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance, L2FeedableBeastInstance),
    	L2FriendlyMobInstance(L2Object, L2Character, L2Npc, L2Attackable),
    	L2PenaltyMonsterInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2RiftInvaderInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2MinionInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2RaidBossInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2GrandBossInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance, L2RaidBossInstance),
    	// FlyMobs
    	L2FlyNpcInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2FlyMonsterInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	L2FlyMinionInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance, L2MinionInstance),
    	L2FlyRaidBossInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance, L2RaidBossInstance),
    	// Sepulchers
    	L2SepulcherNpcInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2SepulcherMonsterInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	// Festival
    	L2FestivalGiudeInstance(L2Object, L2Character, L2Npc),
    	L2FestivalMonsterInstance(L2Object, L2Character, L2Npc, L2Attackable, L2MonsterInstance),
    	// Ships and controllers
    	L2BoatInstance(L2Object, L2Character),
    	L2AirShipInstance(L2Object, L2Character),
    	L2AirShipControllerInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	// Siege
    	L2DefenderInstance(L2Object, L2Character, L2Npc, L2Attackable),
    	L2ArtefactInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2ControlTowerInstance(L2Object, L2Character, L2Npc),
    	L2FlameTowerInstance(L2Object, L2Character, L2Npc),
    	L2SiegeFlagInstance(L2Object, L2Character, L2Npc),
    	L2SiegeNpcInstance(L2Object, L2Character, L2Npc),
    	// Fort Siege
    	L2FortBallistaInstance(L2Object, L2Character, L2Npc),
    	L2FortCommanderInstance(L2Object, L2Character, L2Npc, L2Attackable, L2DefenderInstance),
    	// Castle NPCs
    	L2CastleBlacksmithInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2CastleChamberlainInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2CastleMagicianInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2CastleTeleporterInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2CastleWarehouseInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2MercManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	// Fort NPCs
    	L2FortEnvoyInstance(L2Object, L2Character, L2Npc),
    	L2FortLogisticsInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2FortManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2FortSiegeNpcInstance(L2Object, L2Character, L2Npc),
    	L2FortSupportCaptainInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	// Seven Signs
    	L2CabaleBufferInstance(L2Object, L2Character, L2Npc),
    	L2SignsPriestInstance(L2Object, L2Character, L2Npc),
    	L2DawnPriestInstance(L2Object, L2Character, L2Npc, L2SignsPriestInstance),
    	L2DuskPriestInstance(L2Object, L2Character, L2Npc, L2SignsPriestInstance),
    	L2DungeonGatekeeperInstance(L2Object, L2Character, L2Npc),
    	// City NPCs
    	L2AdventurerInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2AuctioneerInstance(L2Object, L2Character, L2Npc),
    	L2ClanHallManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2ClanTraderInstance(L2Object, L2Character, L2Npc),
    	L2FameManagerInstance(L2Object, L2Character, L2Npc),
    	L2FishermanInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2ManorManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2MercenaryManagerInstance(L2Object, L2Character, L2Npc),
    	L2NpcWalkerInstance(L2Object, L2Character, L2Npc),
    	L2ObservationInstance(L2Object, L2Character, L2Npc),
    	L2OlympiadManagerInstance(L2Object, L2Character, L2Npc),
    	L2PetManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2RaceManagerInstance(L2Object, L2Character, L2Npc),
    	L2SymbolMakerInstance(L2Object, L2Character, L2Npc),
    	L2TeleporterInstance(L2Object, L2Character, L2Npc),
    	L2TownPetInstance(L2Object, L2Character, L2Npc),
    	L2TrainerInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2TransformManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2MerchantInstance),
    	L2VillageMasterInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2WarehouseInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2WyvernManagerInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2XmassTreeInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	// Doormens
    	L2DoormenInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2CastleDoormenInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2DoormenInstance),
    	L2FortDoormenInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2DoormenInstance),
    	L2ClanHallDoormenInstance(L2Object, L2Character, L2Npc, L2NpcInstance, L2DoormenInstance),
    	// Custom
    	L2ClassMasterInstance(L2Object, L2Character, L2Npc, L2NpcInstance),
    	L2NpcBufferInstance(L2Object, L2Character, L2Npc),
    	L2TvTEventNpcInstance(L2Object, L2Character, L2Npc),
    	L2WeddingManagerInstance(L2Object, L2Character, L2Npc);

    	private final long _type;

    	private InstanceType(InstanceType... parents)
    	{
    		long type = 1 << this.ordinal();
    		if (type == 0)
    			throw new Error("Too many instance types, failed to load " + this.name());

    		for (InstanceType i : parents)
    			type |= 1 << i.ordinal();

    		_type = type;
    	}

    	public final boolean isType(InstanceType i)
    	{
    		return (_type & i._type) > 0;
    	}
    }

    protected final void setInstanceType(InstanceType i)
    {
		_instanceType = i;
    }

    public final boolean isInstanceType(InstanceType i)
    {
    	return _instanceType.isType(i);
    }

    // =========================================================
    // Event - Public
    public final void onAction(L2PcInstance player)
    {
    	onAction(player, true);
    }
    
	public void onAction(L2PcInstance player, boolean interact)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Deprecated
    public void onActionShift(L2GameClient client)
    {
        client.getActiveChar().sendPacket(ActionFailed.STATIC_PACKET);
    }
    
	public void onActionShift(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

    public void onForcedAttack(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    /**
     * Do Nothing.<BR><BR>
     *
     * <B><U> Overridden in </U> :</B><BR><BR>
     * <li> L2GuardInstance :  Set the home location of its L2GuardInstance </li>
     * <li> L2Attackable    :  Reset the Spoiled flag </li><BR><BR>
     *
     */
    public void onSpawn()
    {
    }

    // =========================================================
    // Position - Should remove to fully move to L2ObjectPosition
    public final void setXYZ(int x, int y, int z)
    {
        getPosition().setXYZ(x, y, z);
    }

    public final void setXYZInvisible(int x, int y, int z)
    {
        getPosition().setXYZInvisible(x, y, z);
    }

    public final int getX()
    {
    	assert getPosition().getWorldRegion() != null || _isVisible;
        return getPosition().getX();
    }
	/**
	* @return The id of the instance zone the object is in - id 0 is global
	* since everything like dropped items, mobs, players can be in a instanciated area, it must be in l2object
	*/
	public int getInstanceId()
	{
		return _instanceId;
	}

	/**
	* @param instanceId The id of the instance zone the object is in - id 0 is global
	*/
	public void setInstanceId(int instanceId)
	{
		if (_instanceId == instanceId)
			return;
		
		Instance oldI = InstanceManager.getInstance().getInstance(_instanceId);
		Instance newI = InstanceManager.getInstance().getInstance(instanceId);
		
		if (newI == null)
			return;
		
		if (this instanceof L2PcInstance)
		{
			if (_instanceId > 0 && oldI != null)
				oldI.removePlayer(getObjectId());
			if (instanceId > 0)
				newI.addPlayer(getObjectId());
			
			if (((L2PcInstance)this).getPet() != null)
				((L2PcInstance)this).getPet().setInstanceId(instanceId);
		}
		else if (this instanceof L2Npc)
		{
			if (_instanceId > 0 && oldI != null)
				oldI.removeNpc(((L2Npc)this));
			if (instanceId > 0)
				newI.addNpc(((L2Npc)this));
		}
		
		_instanceId = instanceId;

		// If we change it for visible objects, me must clear & revalidate knownlists
		if (_isVisible && _knownList != null)
		{
			if (this instanceof L2PcInstance)
			{
				
				// We don't want some ugly looking disappear/appear effects, so don't update
				// the knownlist here, but players usually enter instancezones through teleporting
				// and the teleport will do the revalidation for us.
			}
			else
			{
				decayMe();
				spawnMe();
			}
		}
	}

    public final int getY()
    {
        assert getPosition().getWorldRegion() != null || _isVisible;
        return getPosition().getY();
    }

    public final int getZ()
    {
        assert getPosition().getWorldRegion() != null || _isVisible;
        return getPosition().getZ();
    }

    // =========================================================
    // Method - Public
    /**
     * Remove a L2Object from the world.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Remove the L2Object from the world</li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR><BR>
     *
     * <B><U> Assert </U> :</B><BR><BR>
     * <li> _worldRegion != null <I>(L2Object is visible at the beginning)</I></li><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR>
     * <li> Delete NPC/PC or Unsummon</li><BR><BR>
     *
     */
    public final void decayMe()
    {
        assert getPosition().getWorldRegion() != null;

        L2WorldRegion reg = getPosition().getWorldRegion();

        synchronized (this)
        {
            _isVisible = false;
            getPosition().setWorldRegion(null);
        }

        // this can synchronize on others instancies, so it's out of
        // synchronized, to avoid deadlocks
        // Remove the L2Object from the world
        L2World.getInstance().removeVisibleObject(this, reg);
        L2World.getInstance().removeObject(this);
        if (Config.SAVE_DROPPED_ITEM)
        	ItemsOnGroundManager.getInstance().removeObject(this);
    }

    public void refreshID()
    {
        L2World.getInstance().removeObject(this);
        IdFactory.getInstance().releaseId(getObjectId());
        _objectId = IdFactory.getInstance().getNextId();
    }

    /**
     * Init the position of a L2Object spawn and add it in the world as a visible object.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Set the x,y,z position of the L2Object spawn and update its _worldregion </li>
     * <li>Add the L2Object spawn in the _allobjects of L2World </li>
     * <li>Add the L2Object spawn to _visibleObjects of its L2WorldRegion</li>
     * <li>Add the L2Object spawn in the world as a <B>visible</B> object</li><BR><BR>
     *
     * <B><U> Assert </U> :</B><BR><BR>
     * <li> _worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR>
     * <li> Create Door</li>
     * <li> Spawn : Monster, Minion, CTs, Summon...</li><BR>
     *
     */
    public final void spawnMe()
    {
        assert getPosition().getWorldRegion() == null && getPosition().getWorldPosition().getX() != 0 && getPosition().getWorldPosition().getY() != 0 && getPosition().getWorldPosition().getZ() != 0;

        synchronized (this)
        {
            // Set the x,y,z position of the L2Object spawn and update its _worldregion
            _isVisible = true;
            getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));

            // Add the L2Object spawn in the _allobjects of L2World
            L2World.getInstance().storeObject(this);

            // Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
            getPosition().getWorldRegion().addVisibleObject(this);
        }

        // this can synchronize on others instancies, so it's out of
        // synchronized, to avoid deadlocks
        // Add the L2Object spawn in the world as a visible object
        L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());

        onSpawn();
    }

    public final void spawnMe(int x, int y, int z)
    {
        assert getPosition().getWorldRegion() == null;

        synchronized (this)
        {
            // Set the x,y,z position of the L2Object spawn and update its _worldregion
            _isVisible = true;

            if (x > L2World.MAP_MAX_X) x = L2World.MAP_MAX_X - 5000;
            if (x < L2World.MAP_MIN_X) x = L2World.MAP_MIN_X + 5000;
            if (y > L2World.MAP_MAX_Y) y = L2World.MAP_MAX_Y - 5000;
            if (y < L2World.MAP_MIN_Y) y = L2World.MAP_MIN_Y + 5000;

            getPosition().setWorldPosition(x, y ,z);
            getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));

            // Add the L2Object spawn in the _allobjects of L2World
        }

        L2World.getInstance().storeObject(this);

        // these can synchronize on others instancies, so they're out of
        // synchronized, to avoid deadlocks

        // Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
        getPosition().getWorldRegion().addVisibleObject(this);

        // Add the L2Object spawn in the world as a visible object
        L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());

        onSpawn();
    }

    public void toggleVisible()
    {
        if (isVisible())
            decayMe();
        else
            spawnMe();
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public boolean isAttackable()
    {
        return false;
    }

    public abstract boolean isAutoAttackable(L2Character attacker);

    public boolean isMarker()
    {
        return false;
    }

    /**
     * Return the visibilty state of the L2Object. <BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * A L2Object is visble if <B>__IsVisible</B>=true and <B>_worldregion</B>!=null <BR><BR>
     */
    public final boolean isVisible()
    {
        //return getPosition().getWorldRegion() != null && _IsVisible;
        return getPosition().getWorldRegion() != null;
    }
    public final void setIsVisible(boolean value)
    {
        _isVisible = value;
        if (!_isVisible) getPosition().setWorldRegion(null);
    }

    public ObjectKnownList getKnownList()
    {
        return _knownList;
    }
    
    /**
     * Initializes the KnownList of the L2Object,
     * is overwritten in classes that require a different knownlist Type.
     * 
     * Removes the need for instanceof checks.
     */
    public void initKnownList()
    {
    	_knownList = new ObjectKnownList(this);
    }
    
    public final void setKnownList(ObjectKnownList value)
    {
    	_knownList = value;
    }

    public final String getName()
    {
        return _name;
    }
    public final void setName(String value)
    {
        _name = value;
    }

    public final int getObjectId()
    {
        return _objectId;
    }

    public final ObjectPoly getPoly()
    {
        if (_poly == null) _poly = new ObjectPoly(this);
        return _poly;
    }

    public ObjectPosition getPosition()
    {
        return _position;
    }
    
    /**
     * Initializes the Position class of the L2Object,
     * is overwritten in classes that require a different position Type.
     * 
     * Removes the need for instanceof checks.
     */
    public void initPosition()
    {
    	_position = new ObjectPosition(this);
    }
    public final void setObjectPosition(ObjectPosition value)
    {
    	_position = value;
    }

    /**
     * returns reference to region this object is in
     */
    public L2WorldRegion getWorldRegion()
    {
        return getPosition().getWorldRegion();
    }

    public L2PcInstance getActingPlayer()
    {
        return null;
    }

    /**
     * Sends the Server->Client info packet for the object.<br><br>
     * Is Overridden in:
     * <li>L2AirShipInstance</li>
     * <li>L2BoatInstance</li>
     * <li>L2DoorInstance</li>
     * <li>L2PcInstance</li>
     * <li>L2StaticObjectInstance</li>
     * <li>L2Decoy</li>
     * <li>L2Npc</li>
     * <li>L2Summon</li>
     * <li>L2Trap</li>
     * <li>L2ItemInstance</li>
     */
    public void sendInfo(L2PcInstance activeChar)
    {
    	
    }

    @Override
    public String toString()
    {
        return (getClass().getSimpleName() + ":"+getName()+"[" + getObjectId() + "]");
    }
    
	/**
	 * Not Implemented.<BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2PcInstance</li><BR><BR>
	 */
	public void sendPacket(L2GameServerPacket mov)
	{
		// default implementation
	}
}