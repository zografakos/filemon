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
package com.l2jserver.gameserver.model.actor;

import static com.l2jserver.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.text.DateFormat;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.SevenSignsFestival;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.HelperBuffTable;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SpawnTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.instancemanager.TownManager;
import com.l2jserver.gameserver.instancemanager.games.Lottery;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2DropCategory;
import com.l2jserver.gameserver.model.L2DropData;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Multisell;
import com.l2jserver.gameserver.model.L2NpcAIData;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.MobGroupTable;
import com.l2jserver.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2ControllableMobInstance;
import com.l2jserver.gameserver.model.actor.instance.L2DoormenInstance;
import com.l2jserver.gameserver.model.actor.instance.L2FestivalGuideInstance;
import com.l2jserver.gameserver.model.actor.instance.L2FishermanInstance;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantInstance;
import com.l2jserver.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SummonInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TeleporterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TrainerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2WarehouseInstance;
import com.l2jserver.gameserver.model.actor.knownlist.NpcKnownList;
import com.l2jserver.gameserver.model.actor.stat.NpcStat;
import com.l2jserver.gameserver.model.actor.status.NpcStatus;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.model.zone.type.L2TownZone;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AbstractNpcInfo;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.EtcStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExShowBaseAttributeCancelWindow;
import com.l2jserver.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import com.l2jserver.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.RadarControl;
import com.l2jserver.gameserver.network.serverpackets.ServerObjectInfo;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.ValidateLocation;
import com.l2jserver.gameserver.skills.Stats;
import com.l2jserver.gameserver.taskmanager.DecayTaskManager;
import com.l2jserver.gameserver.templates.L2HelperBuff;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate.AIType;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.skills.L2SkillType;
import com.l2jserver.gameserver.util.Broadcast;
import com.l2jserver.gameserver.util.StringUtil;
import com.l2jserver.util.Rnd;

/**
 * This class represents a Non-Player-Character in the world. It can be a monster or a friendly character.
 * It also uses a template to fetch some static values. The templates are hardcoded in the client, so we can rely on them.<BR><BR>
 *
 * L2Character :<BR><BR>
 * <li>L2Attackable</li>
 * <li>L2BoxInstance</li>
 * <li>L2FolkInstance</li>
 *
 * @version $Revision: 1.32.2.7.2.24 $ $Date: 2005/04/11 10:06:09 $
 */
public class L2Npc extends L2Character
{
	//private static Logger _log = Logger.getLogger(L2NpcInstance.class.getName());

	/** The interaction distance of the L2NpcInstance(is used as offset in MovetoLocation method) */
	public static final int INTERACTION_DISTANCE = 150;

	/** The L2Spawn object that manage this L2NpcInstance */
	private L2Spawn _spawn;

	/** The flag to specify if this L2NpcInstance is busy */
	private boolean _isBusy = false;

	/** The busy message for this L2NpcInstance */
	private String _busyMessage = "";

	/** True if endDecayTask has already been called */
	volatile boolean _isDecayed = false;

	/** True if a Dwarf has used Spoil on this L2NpcInstance */
	private boolean _isSpoil = false;

	/** The castle index in the array of L2Castle this L2NpcInstance belongs to */
	private int _castleIndex = -2;

	/** The fortress index in the array of L2Fort this L2NpcInstance belongs to */
	private int _fortIndex = -2;

	public boolean isEventMob = false;
	private boolean _isInTown = false;

	private int _isSpoiledBy = 0;

	/** Time of last social packet broadcast*/
	private long _lastSocialBroadcast = 0;

	/** Minimum interval between social packets*/
	private int _minimalSocialInterval = 6000;

	protected RandomAnimationTask _rAniTask = null;
	private int _currentLHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentRHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentEnchant; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentCollisionHeight; // used for npc grow effect skills
	private int _currentCollisionRadius; // used for npc grow effect skills
	
    public boolean _soulshotcharged = false;
    public boolean _spiritshotcharged = false;
    private int _soulshotamount = 0;
    private int _spiritshotamount = 0;
    public boolean _ssrecharged = true;
    public boolean _spsrecharged = true;
    
	//AI Recall
    public int getSoulShot()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getSoulShot();

    }
    public int getSpiritShot()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getSpiritShot();

    }
    public int getSoulShotChance()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getSoulShotChance();

    }

    public int getSpiritShotChance()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getSpiritShotChance();

    }
    
    public boolean useSoulShot()
    {
    	if(_soulshotcharged)
    		return true;
    	if(_ssrecharged)
    	{
    		_soulshotamount = getSoulShot();
    		_ssrecharged = false;
    	}
    	else if (_soulshotamount>0)
    	{
        	if (Rnd.get(100) <= getSoulShotChance())
        	{
        		_soulshotamount = _soulshotamount - 1;
        		 Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 2154, 1, 0, 0), 360000);
        		_soulshotcharged = true;
        	}
    	}
    	else return false;

    	return _soulshotcharged;
    }
    public boolean useSpiritShot()
    {
    	
    	if(_spiritshotcharged)
    		return true;
    	else
    	{
        	//_spiritshotcharged = false;
	    	if(_spsrecharged)
	    	{
	    		_spiritshotamount = getSpiritShot();
	    		_spsrecharged = false;
	    	}
	    	else if (_spiritshotamount>0)
	    	{
	        	if (Rnd.get(100) <= getSpiritShotChance())
	        	{
	        		_spiritshotamount = _spiritshotamount - 1;
	    			Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 2061, 1, 0, 0), 360000);
	        		_spiritshotcharged = true;
	        	}    		
	    	}
	    	else return false;
    	}

    	return _spiritshotcharged;
    }
    public int getEnemyRange()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getEnemyRange();

    }
    
    
    
    public String getEnemyClan()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null || AI.getEnemyClan() == null || "".equals(AI.getEnemyClan()))
    			return "none";
    		else
    		return AI.getEnemyClan();

    }
    
    public String getClan()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null || AI.getClan() == null || "".equals(AI.getClan()))
    			return "none";
    		else
    		return AI.getClan();

    }
    
 // GET THE PRIMARY ATTACK
    public int getPrimaryAttack()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getPrimaryAttack();

    }
    
    public int getSkillChance()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 20;
    		else
    		return AI.getSkillChance();

    }
    
    public int getCanMove()
    {
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getCanMove();

    }
    public int getIsChaos()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getIsChaos();

    }
    
    public int getCanDodge()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getDodge();

    }
    public int getSSkillChance()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getShortRangeChance();

    }
    public int getLSkillChance()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getLongRangeChance();

    }
    public int getSwitchRangeChance()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null)
    			return 0;
    		else
    		return AI.getSwitchRangeChance();

    }
    
    public boolean hasLSkill()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null || AI.getLongRangeSkill() == 0)
    			return false;
    		else
    		return true;

    }
    
    public boolean hasSSkill()
    {
    	
    	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(this.getTemplate().npcId);
    	L2NpcAIData AI = npcData.getAIDataStatic();

    		if (AI == null || AI.getShortRangeSkill() == 0)
    			return false;
    		else
    		return true;

    }
    
	public FastList<L2Skill> getLrangeSkill()
	{
		FastList<L2Skill> skilldata = new FastList <L2Skill>();
		boolean hasLrange = false;
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(getTemplate().npcId);
		L2NpcAIData AI = npcData.getAIDataStatic();

		if (AI == null || AI.getLongRangeSkill() == 0)
			return null;

		switch (AI.getLongRangeSkill())
		{
			case -1:
			{
				L2Skill[] skills = null;
				skills = getAllSkills();
				if (skills != null)
				{
					for (L2Skill sk: skills)
					{
						if (sk == null || sk.isPassive()
								|| sk.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
							continue;

						if (sk.getCastRange() >= 200)
						{
							skilldata.add(sk);	
							hasLrange = true;
						}
					}
				}
				break;
			}
			case 1:
			{
				if (npcData._universalskills != null)
				{
					for (L2Skill sk: npcData._universalskills)
					{
						if (sk.getCastRange() >= 200)
						{
							skilldata.add(sk);	
							hasLrange = true;
						}
					}
				}
				break;
			}
			default:
			{
				for (L2Skill sk: getAllSkills())
				{
					if (sk.getId() == AI.getLongRangeSkill())
					{
						skilldata.add(sk);	
						hasLrange = true;
					}
				}
			}
		}

		return (hasLrange ? skilldata : null);
	}

	public FastList<L2Skill> getSrangeSkill()
	{
		FastList<L2Skill> skilldata = new FastList <L2Skill>();
		boolean hasSrange = false;
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(getTemplate().npcId);
		L2NpcAIData AI = npcData.getAIDataStatic();

		if (AI == null || AI.getShortRangeSkill() == 0)
			return null;

		switch (AI.getShortRangeSkill())
		{
			case -1:
			{
				L2Skill[] skills = null;
				skills = getAllSkills();
				if (skills != null)
				{
					for (L2Skill sk: skills)
					{
						if (sk == null || sk.isPassive()
								|| sk.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
							continue;

						if (sk.getCastRange() <= 200)
						{
							skilldata.add(sk);
							hasSrange = true;
						}
					}
				}
				break;
			}
			case 1:
			{
				if (npcData._universalskills != null)
				{
					for (L2Skill sk: npcData._universalskills)
					{
						if (sk.getCastRange() <= 200)
						{
							skilldata.add(sk);	
							hasSrange = true;
						}
					}
				}
				break;
			}
			default:
			{
				for (L2Skill sk: getAllSkills())
				{
					if (sk.getId() == AI.getShortRangeSkill())
					{
						skilldata.add(sk);	
						hasSrange = true;
					}
				}
			}
		}

		return (hasSrange ? skilldata : null);
	}
    
	/** Task launching the function onRandomAnimation() */
	protected class RandomAnimationTask implements Runnable
	{
		public void run()
		{
			try
			{
				if (this != _rAniTask)
					return; // Shouldn't happen, but who knows... just to make sure every active npc has only one timer.
				if (isMob())
				{
					// Cancel further animation timers until intention is changed to ACTIVE again.
					if (getAI().getIntention() != AI_INTENTION_ACTIVE)
						return;
				}
				else
				{
					if (!isInActiveRegion()) // NPCs in inactive region don't run this task
						return;
				}

				if (!(isDead() || isStunned() || isSleeping() || isParalyzed()))
					onRandomAnimation();

				startRandomAnimationTimer();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	/**
	 * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance and create a new RandomAnimation Task.<BR><BR>
	 */
	public void onRandomAnimation()
	{
		// Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		long now = System.currentTimeMillis();
		if (now - _lastSocialBroadcast > _minimalSocialInterval)
		{
			_lastSocialBroadcast = now;
			broadcastPacket(new SocialAction(getObjectId(), Rnd.get(2, 3)));
		}
	}

	/**
	 * Create a RandomAnimation Task that will be launched after the calculated delay.<BR><BR>
	 */
	public void startRandomAnimationTimer()
	{
		if (!hasRandomAnimation())
			return;

		int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION : Config.MIN_NPC_ANIMATION;
		int maxWait = isMob() ? Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;

		// Calculate the delay before the next animation
		int interval = Rnd.get(minWait, maxWait) * 1000;

		// Create a RandomAnimation Task that will be launched after the calculated delay
		_rAniTask = new RandomAnimationTask();
		ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
	}

	/**
	 * Check if the server allows Random Animation.<BR><BR>
	 */
	public boolean hasRandomAnimation()
	{
		return (Config.MAX_NPC_ANIMATION > 0 && !getTemplate().AI.equals(AIType.CORPSE));
	}

	/**
	 * Constructor of L2NpcInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Character (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)  </li>
	 * <li>Set the name of the L2Character</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2NpcTemplate to apply to the NPC
	 *
	 */
	public L2Npc(int objectId, L2NpcTemplate template)
	{
		// Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(objectId, template);
		initCharStatusUpdateValues();

		// initialize the "current" equipment
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		_currentEnchant = Config.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4,21) : getTemplate().enchantEffect;
		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;

		if (template == null)
		{
			_log.severe("No template for Npc. Please check your datapack is setup correctly.");
			return;
		}

		// Set the name of the L2Character
		setName(template.name);
	}

	@Override
	public NpcKnownList getKnownList()
	{
		return (NpcKnownList) super.getKnownList();
	}
	
	@Override
    public void initKnownList()
    {
		setKnownList(new NpcKnownList(this));
    }

	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}

	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}

	/** Return the L2NpcTemplate of the L2NpcInstance. */
	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}

	/**
	 * Return the generic Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getNpcId()
	{
		return getTemplate().npcId;
	}

	@Override
	public boolean isAttackable()
	{
		return Config.ALT_ATTACKABLE_NPCS;
	}

	/**
	 * Return the faction Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * If a NPC belows to a Faction, other NPC of the faction inside the Faction range will help it if it's attacked<BR><BR>
	 *
	 */
	public final String getFactionId()
	{
		return getTemplate().factionId;
	}

	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public final int getLevel()
	{
		return getTemplate().level;
	}

	/**
	 * Return True if the L2NpcInstance is agressive (ex : L2MonsterInstance in function of aggroRange).<BR><BR>
	 */
	public boolean isAggressive()
	{
		return false;
	}

	/**
	 * Return the Aggro Range of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getAggroRange()
	{
		return getTemplate().aggroRange;
	}

	/**
	 * Return the Faction Range of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getFactionRange()
	{
		return getTemplate().factionRange;
	}

	/**
	 * Return True if this L2NpcInstance is undead in function of the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead;
	}

	/**
	 * Send a packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance.<BR><BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		// Send a Server->Client packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				if (getRunSpeed() == 0)
					player.sendPacket(new ServerObjectInfo(this, player));
				else
					player.sendPacket(new AbstractNpcInfo.NpcInfo(this, player));
			}
		}
	}

	/**
	 * Return the distance under which the object must be add to _knownObject in
	 * function of the object type.<BR>
	 * <BR>
	 * 
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li> object is a L2FolkInstance : 0 (don't remember it) </li>
	 * <li> object is a L2Character : 0 (don't remember it) </li>
	 * <li> object is a L2PlayableInstance : 1500 </li>
	 * <li> others : 500 </li>
	 * <BR>
	 * <BR>
	 * 
	 * <B><U> Override in </U> :</B><BR>
	 * <BR>
	 * <li> L2Attackable</li>
	 * <BR>
	 * <BR>
	 * 
	 * @param object
	 *            The Object to add to _knownObject
	 * 
	 */
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2FestivalGuideInstance)
			return 10000;

		if (object instanceof L2NpcInstance || !(object instanceof L2Character))
			return 0;

		if (object instanceof L2Playable)
			return 1500;

		return 500;
	}

	/**
	 * Return the distance after which the object must be remove from _knownObject in function of the object type.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li> object is not a L2Character : 0 (don't remember it) </li>
	 * <li> object is a L2FolkInstance : 0 (don't remember it)</li>
	 * <li> object is a L2PlayableInstance : 3000 </li>
	 * <li> others : 1000 </li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2Attackable</li><BR><BR>
	 *
	 * @param object The Object to remove from _knownObject
	 *
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}

	/**
	 * Return False.<BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2MonsterInstance : Check if the attacker is not another L2MonsterInstance</li>
	 * <li> L2PcInstance</li><BR><BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	/**
	 * Return the Identifier of the item in the left hand of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	/**
	 * Return the Identifier of the item in the right hand of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	public int getEnchantEffect()
	{
		return _currentEnchant;
	}

	/**
	 * Return True if this L2NpcInstance has drops that can be sweeped.<BR><BR>
	 */
	public boolean isSpoil()
	{
		return _isSpoil;
	}

	/**
	 * Set the spoil state of this L2NpcInstance.<BR><BR>
	 */
	public void setSpoil(boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}

	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}

	public final void setIsSpoiledBy(int value)
	{
		_isSpoiledBy = value;
	}

	/**
	 * Return the busy status of this L2NpcInstance.<BR><BR>
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}

	/**
	 * Set the busy status of this L2NpcInstance.<BR><BR>
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}

	/**
	 * Return the busy message of this L2NpcInstance.<BR><BR>
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	/**
	 * Set the busy message of this L2NpcInstance.<BR><BR>
	 */
	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}

	/**
	 * Return true if this L2Npc instance can be warehouse manager.<BR><BR> 
	 */
	public boolean isWarehouse()
	{
		return false;
	}

	protected boolean canTarget(L2PcInstance player)
	{
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (player.isLockedTarget() && player.getLockedTarget() != this)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		// TODO: More checks...

		return true;
	}

	public boolean canInteract(L2PcInstance player)
	{
		// TODO: NPC busy check etc...

		if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
			return false;
		if (player.isDead() || player.isFakeDeath())
			return false;
		if (player.isSitting())
			return false;
		if (player.getPrivateStoreType() != 0)
			return false;
		if (!isInsideRadius(player, INTERACTION_DISTANCE, true, false))
			return false;
		if (player.getInstanceId() != getInstanceId()
				&& player.getInstanceId() != -1)
			return false;

		return true;
	}

	/**
	 * Manage actions when a player click on the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions on first click on the L2NpcInstance (Select it)</U> :</B><BR><BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the L2PcInstance in order to update L2NpcInstance HP bar </li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client </li><BR><BR>
	 *
	 * <B><U> Actions on second click on the L2NpcInstance (Attack it/Intercat with it)</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, notify the L2PcInstance AI with AI_INTENTION_ATTACK (after a height verification)</li>
	 * <li>If L2NpcInstance is NOT autoAttackable, notify the L2PcInstance AI with AI_INTENTION_INTERACT (after a distance verification) and show message</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid
	 * that client wait an other packet</B></FONT><BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : Action, AttackRequest</li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2ArtefactInstance : Manage only fisrt click to select Artefact</li><BR><BR>
	 * <li> L2GuardInstance : </li><BR><BR>
	 *
	 * @param player The L2PcInstance that start an action on the L2NpcInstance
	 *
	 */
	@Override
	public void onAction(L2PcInstance player, boolean interact)
	{
		if (!canTarget(player))
			return;

		player.setLastFolkNPC(this);

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			if (Config.DEBUG)
				_log.fine("new target selected:" + getObjectId());

			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				// The player.getLevel() - getLevel() permit to display the correct color in the select window
				MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
				player.sendPacket(my);

				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			else
			{
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
				player.sendPacket(my);
			}

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else if (interact)
		{
			player.sendPacket(new ValidateLocation(this));
			// Check if the player is attackable (without a forced attack) and isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					// player.startAttack(this);
				}
				else
				{
					// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			else if (!isAutoAttackable(player))
			{
				// Calculate the distance between the L2PcInstance and the L2NpcInstance
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
					// to display a social action of the L2NpcInstance on their client
					long now = System.currentTimeMillis();
					if (now - _lastSocialBroadcast > _minimalSocialInterval && !getTemplate().AI.equals(AIType.CORPSE))
					{
						_lastSocialBroadcast = now;
						broadcastPacket(new SocialAction(getObjectId(), Rnd.get(8)));
					}

					// Open a chat window on client with the text of the L2NpcInstance
					if (isEventMob)
					{
						L2Event.showEventHtml(player, String.valueOf(getObjectId()));
					}
					else
					{
						Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
						if ((qlsa != null) && qlsa.length > 0)
							player.setLastQuestNpcObject(getObjectId());
						Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
						if ((qlst != null) && qlst.length == 1)
							qlst[0].notifyFirstTalk(this, player);
						else
							showChatWindow(player);
					}
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Manage and Display the GM console to modify the L2NpcInstance (GM only).<BR><BR>
	 * 
	 * <B><U> Actions (If the L2PcInstance is a GM only)</U> :</B><BR><BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the L2PcInstance in order to update L2NpcInstance HP bar </li>
	 * <li>Send a Server->Client NpcHtmlMessage() containing the GM console about this L2NpcInstance </li><BR><BR>
	 * 
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid
	 * that client wait an other packet</B></FONT><BR><BR>
	 * 
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : Action</li><BR><BR>
	 * 
	 * @param client The thread that manage the player that pessed Shift and click on the L2NpcInstance
	 * 
	 */
	@Override
	public void onActionShift(L2PcInstance player)
	{
		if (player == null)
			return;

		// Check if the L2PcInstance is a GM
		if (player.getAccessLevel().isGm())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The player.getLevel() - getLevel() permit to display the correct color in the select window
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			// Send a Server->Client NpcHtmlMessage() containing the GM console about this L2NpcInstance
			NpcHtmlMessage html = new NpcHtmlMessage(0);
                        final StringBuilder html1 = StringUtil.startAppend(500,
                                "<html><body><center><font color=\"LEVEL\">NPC Info</font></center><br>" +
                                "Instance Type: ",
                                getClass().getSimpleName(),
                                "<br1>Faction: ",
                                getFactionId() != null ? getFactionId() : "null"
                                );
                        StringUtil.append(html1,
                        		"<br1>Coords: ",
                        		String.valueOf(getX()),
                        		", ",
                        		String.valueOf(getY()),
                        		", ",
                        		String.valueOf(getZ())
                        		);
                        if (getSpawn() != null)
                        	StringUtil.append(html1,
                        			"<br1>Spawn: ",
                        			String.valueOf(getSpawn().getLocx()),
                        			", ",
                        			String.valueOf(getSpawn().getLocy()),
                        			", ",
                        			String.valueOf(getSpawn().getLocz()),
                                    " ; Loc ID: ",
                                    String.valueOf(getSpawn().getLocation()),
                                    "<br1>Distance from spawn 2D: ",
                                    String.valueOf((int)Math.sqrt(getPlanDistanceSq(getSpawn().getLocx(), getSpawn().getLocy()))),
                                    " ; 3D: ",
                                    String.valueOf((int)Math.sqrt(getDistanceSq(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz())))
                            );

			if (this instanceof L2ControllableMobInstance)
			{
				StringUtil.append(html1,
						"<br1>Mob Group: ",
						String.valueOf(MobGroupTable.getInstance().getGroupForMob((L2ControllableMobInstance) this).getGroupId()),
						"<br>"
				);
			}
			else
			{
				StringUtil.append(html1,
						"<br1>Respawn Time: ",
						(getSpawn() != null ? String.valueOf(getSpawn().getRespawnDelay() / 1000) : "?"),
						"  Seconds<br>"
				);
			}

			StringUtil.append(html1,
					"<table border=\"0\" width=\"100%\">" +
					"<tr><td>Level</td><td>",
					String.valueOf(getLevel()),
					"</td><td>    </td><td>NPC ID</td><td>",
					String.valueOf(getTemplate().npcId),
					"</td></tr>" +
					"<tr><td>Aggro</td><td>" +
					String.valueOf((this instanceof L2Attackable) ? ((L2Attackable) this).getAggroRange() : 0),
					"</td><td>    </td><td>Object ID</td><td>",
					String.valueOf(getObjectId()),
					"</td></tr>" +
					"<tr><td>Castle</td><td>",
					String.valueOf(getCastle().getCastleId()),
					"</td><td>    </td><td>AI </td><td>",
					(hasAI() ? String.valueOf(getAI().getIntention().name()) : "NULL"),
					"</td></tr>" +
					"</table><br>" +
					"<font color=\"LEVEL\">Combat</font>" +
					"<table border=\"0\" width=\"100%\">" +
					"<tr><td>Current HP</td><td>",
					String.valueOf(getCurrentHp()),
					"</td><td>Current MP</td><td>",
					String.valueOf(getCurrentMp()),
					"</td></tr>" +
					"<tr><td>Max.HP</td><td>",
					String.valueOf((int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null))),
					"*",
					String.valueOf((int) (getStat().calcStat(Stats.MAX_HP, 1, this, null))),
					"</td><td>Max.MP</td><td>",
					String.valueOf(getMaxMp()),
					"</td></tr>" +
					"<tr><td>P.Atk.</td><td>",
					String.valueOf(getPAtk(null)),
					"</td><td>M.Atk.</td><td>",
					String.valueOf(getMAtk(null, null)),
					"</td></tr>" +
					"<tr><td>P.Def.</td><td>",
					String.valueOf(getPDef(null)),
					"</td><td>M.Def.</td><td>",
					String.valueOf(getMDef(null, null)),
					"</td></tr>" +
					"<tr><td>Accuracy</td><td>" +
					String.valueOf(getAccuracy()),
					"</td><td>Evasion</td><td>",
					String.valueOf(getEvasionRate(null)),
					"</td></tr>" +
					"<tr><td>Critical</td><td>",
					String.valueOf(getCriticalHit(null, null)),
					"</td><td>Speed</td><td>",
					String.valueOf(getRunSpeed()),
					"</td></tr>" +
					"<tr><td>Atk.Speed</td><td>",
					String.valueOf(getPAtkSpd()),
					"</td><td>Cast.Speed</td><td>",
					String.valueOf(getMAtkSpd()),
					"</td></tr>" +
					"</table><br>" +
					"<font color=\"LEVEL\">Basic Stats</font>" +
					"<table border=\"0\" width=\"100%\">" +
					"<tr><td>STR</td><td>",
					String.valueOf(getSTR()),
					"</td><td>DEX</td><td>",
					String.valueOf(getDEX()),
					"</td><td>CON</td><td>",
					String.valueOf(getCON()),
					"</td></tr>" +
					"<tr><td>INT</td><td>",
					String.valueOf(getINT()),
					"</td><td>WIT</td><td>",
					String.valueOf(getWIT()),
					"</td><td>MEN</td><td>",
					String.valueOf(getMEN()),
					"</td></tr>" +
					"</table>" +
					"<br><center><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1></td>" +
					"<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><br1></tr>" +
					"<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<tr><td><button value=\"Show SkillList\" action=\"bypass -h admin_show_skilllist_npc ",
					String.valueOf(getTemplate().npcId),
				 	"\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td></td></tr></table></center><br></body></html>"
					);

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else if (Config.ALT_GAME_VIEWNPC)
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The player.getLevel() - getLevel() permit to display the correct color in the select window
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(0);
			final StringBuilder html1 = StringUtil.startAppend(
					1000,
					"<html><body>" +
					"<br><center><font color=\"LEVEL\">[Combat Stats]</font></center>" +
					"<table border=0 width=\"100%\">" +
					"<tr><td>Max.HP</td><td>",
					String.valueOf((int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null))),
					"*",
					String.valueOf((int) getStat().calcStat(Stats.MAX_HP, 1, this, null)),
					"</td><td>Max.MP</td><td>",
					String.valueOf(getMaxMp()),
					"</td></tr>" +
					"<tr><td>P.Atk.</td><td>",
					String.valueOf(getPAtk(null)),
					"</td><td>M.Atk.</td><td>",
					String.valueOf(getMAtk(null, null)),
					"</td></tr>" +
					"<tr><td>P.Def.</td><td>",
					String.valueOf(getPDef(null)),
					"</td><td>M.Def.</td><td>",
					String.valueOf(getMDef(null, null)),
					"</td></tr>" +
					"<tr><td>Accuracy</td><td>",
					String.valueOf(getAccuracy()),
					"</td><td>Evasion</td><td>",
					String.valueOf(getEvasionRate(null)),
					"</td></tr>" +
					"<tr><td>Critical</td><td>",
					String.valueOf(getCriticalHit(null, null)),
					"</td><td>Speed</td><td>",
					String.valueOf(getRunSpeed()),
					"</td></tr>" +
					"<tr><td>Atk.Speed</td><td>",
					String.valueOf(getPAtkSpd()),
					"</td><td>Cast.Speed</td><td>",
					String.valueOf(getMAtkSpd()),
					"</td></tr>" +
					"<tr><td>Race</td><td>",
					getTemplate().getRace().toString(),
					"</td><td></td><td></td></tr>" +
					"</table>" +
					"<br><center><font color=\"LEVEL\">[Basic Stats]</font></center>" +
					"<table border=0 width=\"100%\">" +
					"<tr><td>STR</td><td>",
					String.valueOf(getSTR()),
					"</td><td>DEX</td><td>",
					String.valueOf(getDEX()),
					"</td><td>CON</td><td>",
					String.valueOf(getCON()),
					"</td></tr>" +
					"<tr><td>INT</td><td>",
					String.valueOf(getINT()),
					"</td><td>WIT</td><td>",
					String.valueOf(getWIT()),
					"</td><td>MEN</td><td>",
					String.valueOf(getMEN()),
					"</td></tr>" +
					"</table>"
					);

			if (getTemplate().getDropData() != null)
			{
				StringUtil.append(html1,
						"<br><center><font color=\"LEVEL\">[Drop Info]</font></center>" +
						"<br>Rates legend: <font color=\"ff0000\">50%+</font> <font color=\"00ff00\">30%+</font> <font color=\"0000ff\">less than 30%</font>" +
						"<table border=0 width=\"100%\">"
						);
				for (L2DropCategory cat : getTemplate().getDropData())
				{
					for (L2DropData drop : cat.getAllDrops())
					{
						final L2Item item = ItemTable.getInstance().getTemplate(drop.getItemId());
						if (item == null)
							continue;

						final String color;

						if (drop.getChance() >= 500000)
							color = "ff0000";
						else if (drop.getChance() >= 300000)
							color = "00ff00";
						else
							color = "0000ff";

						StringUtil.append(html1,
								"<tr><td><font color=\"",
								color,
								"\">",
								item.getName(),
								"</font></td><td>",
								(drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")),
								"</td></tr>"
								);
					}
				}
				html1.append("</table>");
			}
			html1.append("</body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/** Return the L2Castle this L2NpcInstance belongs to. */
	public final Castle getCastle()
	{
		// Get castle this NPC belongs to (excluding L2Attackable)
		if (_castleIndex < 0)
		{
			L2TownZone town = TownManager.getTown(getX(), getY(), getZ());

			if (town != null)
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());

			if (_castleIndex < 0)
			{
				_castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
			}
			else
				_isInTown = true; // Npc was spawned in town
		}

		if (_castleIndex < 0)
			return null;

		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}

	/** Return the L2Fort this L2NpcInstance belongs to. */
	public final Fort getFort()
	{
		// Get Fort this NPC belongs to (excluding L2Attackable)
		if (_fortIndex < 0)
		{
			Fort fort = FortManager.getInstance().getFort(getX(), getY(), getZ());
			if (fort != null)
				_fortIndex = FortManager.getInstance().getFortIndex(fort.getFortId());

			if (_fortIndex < 0)
				_fortIndex = FortManager.getInstance().findNearestFortIndex(this);
		}

		if (_fortIndex < 0)
			return null;

		return FortManager.getInstance().getForts().get(_fortIndex);
	}

	public final boolean getIsInTown()
	{
		if (_castleIndex < 0)
			getCastle();

		return _isInTown;
	}

	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : RequestBypassToServer</li><BR><BR>
	 *
	 * @param command The command string received from client
	 *
	 */
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		//if (canInteract(player))
		{
			if (isBusy() && getBusyMessage().length() > 0)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/npcbusy.htm");
				html.replace("%busymessage%", getBusyMessage());
				html.replace("%npcname%", getName());
				html.replace("%playername%", player.getName());
				player.sendPacket(html);
			}
			else if (command.equalsIgnoreCase("TerritoryStatus"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				{
					if (getCastle().getOwnerId() > 0)
					{
						html.setFile("data/html/territorystatus.htm");
						L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
						html.replace("%clanname%", clan.getName());
						html.replace("%clanleadername%", clan.getLeaderName());
					}
					else
					{
						html.setFile("data/html/territorynoclan.htm");
					}
				}
				html.replace("%castlename%", getCastle().getName());
				html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
				html.replace("%objectId%", String.valueOf(getObjectId()));
				{
					if (getCastle().getCastleId() > 6)
					{
						html.replace("%territory%", "The Kingdom of Elmore");
					}
					else
					{
						html.replace("%territory%", "The Kingdom of Aden");
					}
				}
				player.sendPacket(html);
			}
			else if (command.startsWith("Quest"))
			{
				String quest = "";
				try
				{
					quest = command.substring(5).trim();
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				if (quest.length() == 0)
					showQuestWindow(player);
				else
					showQuestWindow(player, quest);
			}
			else if (command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}
				showChatWindow(player, val);
			}
			else if (command.startsWith("Link"))
			{
				String path = command.substring(5).trim();
				if (path.indexOf("..") != -1)
					return;
				String filename = "data/html/" + path;
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else if (command.startsWith("NobleTeleport"))
			{
				if (!player.isNoble())
				{
					String filename = "data/html/teleporter/nobleteleporter-no.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					return;
				}
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}
				showChatWindow(player, val);
			}
			else if (command.startsWith("Loto"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}
				if (val == 0)
				{
					// new loto ticket
					for (int i = 0; i < 5; i++)
						player.setLoto(i, 0);
				}
				showLotoWindow(player, val);
			}
			else if (command.startsWith("CPRecovery"))
			{
				makeCPRecovery(player);
			}
			else if (command.startsWith("SupportMagicServitor"))
			{
				makeSupportMagic(player,true);
			}
			else if (command.startsWith("SupportMagic"))
			{
				makeSupportMagic(player,false);
			}
			else if (command.startsWith("GiveBlessing"))
			{
				giveBlessingSupport(player);
			}
			else if (command.startsWith("multisell"))
			{
				int listId = Integer.parseInt(command.substring(9).trim());
				L2Multisell.getInstance().separateAndSend(listId, player, getNpcId(), false, getCastle().getTaxRate());
			}
			else if (command.startsWith("exc_multisell"))
			{
				int listId = Integer.parseInt(command.substring(13).trim());
				L2Multisell.getInstance().separateAndSend(listId, player, getNpcId(), true, getCastle().getTaxRate());
			}
			else if (command.startsWith("Augment"))
			{
				int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
				switch (cmdChoice)
				{
					case 1:
						player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
						player.sendPacket(new ExShowVariationMakeWindow());
						break;
					case 2:
						player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
						player.sendPacket(new ExShowVariationCancelWindow());
						break;
				}
			}
			else if (command.startsWith("npcfind_byid"))
			{
				try
				{
					L2Spawn spawn = SpawnTable.getInstance().getTemplate(Integer.parseInt(command.substring(12).trim()));
					
					if (spawn != null)
					{
						player.sendPacket(new RadarControl(2, 2, spawn.getLocx(), spawn.getLocy(), spawn.getLocz()));
						player.sendPacket(new RadarControl(0, 1, spawn.getLocx(), spawn.getLocy(), spawn.getLocz()));
					}
				}
				catch (NumberFormatException nfe)
				{
					player.sendMessage("Wrong command parameters");
				}
			}
			else if (command.startsWith("EnterRift"))
			{
				try
				{
					Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
					DimensionalRiftManager.getInstance().start(player, b1, this);
				}
				catch (Exception e)
				{
				}
			}
			else if (command.startsWith("ChangeRiftRoom"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
				{
					player.getParty().getDimensionalRift().manualTeleport(player, this);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, this);
				}
			}
			else if (command.startsWith("remove_dp"))
			{
				int cmdChoice = Integer.parseInt(command.substring(10, 11).trim());
				int[] pen_clear_price =
				{
					3600, 8640, 25200, 50400, 86400, 144000, 144000, 144000
				};
				switch (cmdChoice)
				{
					case 1:
						String filename = "data/html/default/30981-1.htm";
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(filename);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%dp_price%", String.valueOf(pen_clear_price[player.getExpertiseIndex()]));
						player.sendPacket(html);
						break;
					case 2:
						NpcHtmlMessage Reply = new NpcHtmlMessage(getObjectId());
                                                final StringBuilder replyMSG = StringUtil.startAppend(400,
                                                        "<html><body>Black Judge:<br>"
                                                        );

						if (player.getDeathPenaltyBuffLevel() > 0)
						{
							if (player.getAdena() >= pen_clear_price[player.getExpertiseIndex()])
							{
								if (!player.reduceAdena("DeathPenality", pen_clear_price[player.getExpertiseIndex()], this, true))
									return;
								player.setDeathPenaltyBuffLevel(player.getDeathPenaltyBuffLevel() - 1);
								player.sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
								player.sendPacket(new EtcStatusUpdate(player));
								return;
							}
							else
							{
								replyMSG.append("The wound you have received from death's touch is too deep to be healed for the money you have to give me. Find more money if you wish death's mark to be fully removed from you.");
							}
						}
						else 
						{
							replyMSG.append("You have no more death wounds that require healing.<br>" +
                                                                "Go forth and fight, both for this world and your own glory.");
						}
						
						replyMSG.append("</body></html>");
						Reply.setHtml(replyMSG.toString());
						player.sendPacket(Reply);
						break;
				}
			}
			else if (command.startsWith("ExitRift"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
				{
					player.getParty().getDimensionalRift().manualExitRift(player, this);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, this);
				}
			}
			else if (command.startsWith("ReleaseAttribute"))
			{
				player.sendPacket(new ExShowBaseAttributeCancelWindow(player));
			}
			else 
			{
				_log.info(getClass().getSimpleName()+": Unknown NPC bypass: \""+command+"\" NpcId: "+getNpcId());
			}
		}
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}

	/**
	 * Return the weapon item equiped in the right hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		int weaponId = getTemplate().rhand;

		if (weaponId < 1)
			return null;

		// Get the weapon item equiped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().rhand);

		if (!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	public void giveBlessingSupport(L2PcInstance player)
	{
		if (player == null)
			return;

		// Blessing of protection - author kerberos_20. Used codes from Rayan - L2Emu project.
		// Prevent a cursed weapon weilder of being buffed - I think no need of that becouse karma check > 0
		// if (player.isCursedWeaponEquiped()) 
		//   return; 

		int player_level = player.getLevel();
		// Select the player 
		setTarget(player);
		// If the player is too high level, display a message and return 
		if (player_level > 39 || player.getClassId().level() >= 2)
		{
			String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br1>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer.</font></body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
		doCast(skill);
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}

	/**
	 * Return the weapon item equiped in the left hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		int weaponId = getTemplate().lhand;

		if (weaponId < 1)
			return null;

		// Get the weapon item equiped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().lhand);

		if (!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	/**
	 * Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance.<BR><BR>
	 * 
	 * @param player The L2PcInstance who talks with the L2NpcInstance
	 * @param content The text of the L2NpcMessage
	 * 
	 */
	public void insertObjectIdAndShowChatWindow(L2PcInstance player, String content)
	{
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
	}

	/**
	 * Return the pathfile of the selected HTML file in function of the npcId and of the page number.<BR><BR>
	 *   
	 * <B><U> Format of the pathfile </U> :</B><BR><BR>
	 * <li> if the file exists on the server (page number = 0) : <B>data/html/default/12006.htm</B> (npcId-page number)</li>
	 * <li> if the file exists on the server (page number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page number)</li>
	 * <li> if the file doesn't exist on the server : <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to you")</li><BR><BR>
	 * 
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2GuardInstance : Set the pathfile to data/html/guard/12006-1.htm (npcId-page number)</li><BR><BR>
	 * 
	 * @param npcId The Identifier of the L2NpcInstance whose text must be display
	 * @param val The number of the page to display
	 * 
	 */
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		String temp = "data/html/default/" + pom + ".htm";

		if (!Config.LAZY_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it doesnt exist
			if (HtmCache.getInstance().contains(temp))
				return temp;
		}
		else
		{
			if (HtmCache.getInstance().isLoadable(temp))
				return temp;
		}

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}

	/**
	 * Open a choose quest window on client with all quests available of the L2NpcInstance.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li><BR><BR>
	 * 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param quests The table containing quests of the L2NpcInstance
	 * 
	 */
	public void showQuestChooseWindow(L2PcInstance player, Quest[] quests)
	{
		final StringBuilder sb = StringUtil.startAppend(150,
				"<html><body>"
		);
		for (Quest q : quests)
		{
			StringUtil.append(sb,
					"<a action=\"bypass -h npc_",
					String.valueOf(getObjectId()),
					"_Quest ",
					q.getName(),
					"\">[",
					q.getDescr()
			);

			QuestState qs = player.getQuestState(q.getScriptName());
			if (qs != null)
			{
				if (qs.getState() == State.STARTED && qs.getInt("cond") > 0)
					sb.append(" (In Progress)");
				else if (qs.getState() == State.COMPLETED)
					sb.append(" (Done)");
			}
			sb.append("]</a><br>");
		}

		sb.append("</body></html>");

		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		insertObjectIdAndShowChatWindow(player, sb.toString());
	}

	/**
	 * Open a quest window on client with the text of the L2NpcInstance.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the quest state in the folder data/scripts/quests/questId/stateId.htm </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR><BR>
	 * 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param questId The Identifier of the quest to display the message
	 * 
	 */
	public void showQuestWindow(L2PcInstance player, String questId)
	{
		String content = null;

		Quest q = QuestManager.getInstance().getQuest(questId);

		// Get the state of the selected quest
		QuestState qs = player.getQuestState(questId);

		if (q == null)
		{
			// no quests found
			content = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
		}
		else
		{
			if ((q.getQuestIntId() >= 1 && q.getQuestIntId() < 20000) && (player.getWeightPenalty() >= 3 || player.getInventoryLimit() * 0.8 <= player.getInventory().getSize()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT));
				return;
			}

			if (qs == null)
			{
				if (q.getQuestIntId() >= 1 && q.getQuestIntId() < 20000)
				{
					Quest[] questList = player.getAllActiveQuests();
					if (questList.length >= 25) // if too many ongoing quests, don't show window and send message
					{
						player.sendPacket(new SystemMessage(SystemMessageId.TOO_MANY_QUESTS));
						return;
					}
				}
				// check for start point
				Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

				if (qlst != null && qlst.length > 0)
				{
					for (Quest temp : qlst)
					{
						if (temp == q)
						{
							qs = q.newQuestState(player);
							break;
						}
					}
				}
			}
		}

		if (qs != null)
		{
			// If the quest is alreday started, no need to show a window
			if (!qs.getQuest().notifyTalk(this, qs))
				return;

			questId = qs.getQuest().getName();
			String stateId = State.getStateName(qs.getState());
			String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
			content = HtmCache.getInstance().getHtm(path); //TODO path for quests html

			if (Config.DEBUG)
			{
				if (content != null)
				{
					_log.fine("Showing quest window for quest " + questId + " html path: " + path);
				}
				else
				{
					_log.fine("File not exists for quest " + questId + " html path: " + path);
				}
			}
		}

		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		if (content != null)
			insertObjectIdAndShowChatWindow(player, content);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.<BR><BR>
	 * 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * 
	 */
	public void showQuestWindow(L2PcInstance player)
	{
		// collect awaiting quests and start points
		List<Quest> options = new FastList<Quest>();

		QuestState[] awaits = player.getQuestsForTalk(getTemplate().npcId);
		Quest[] starts = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

		// Quests are limited between 1 and 999 because those are the quests that are supported by the client.  
		// By limiting them there, we are allowed to create custom quests at higher IDs without interfering  
		if (awaits != null)
		{
			for (QuestState x : awaits)
			{
				if (!options.contains(x.getQuest()))
					if ((x.getQuest().getQuestIntId() > 0) && (x.getQuest().getQuestIntId() < 20000))
						options.add(x.getQuest());
			}
		}

		if (starts != null)
		{
			for (Quest x : starts)
			{
				if (!options.contains(x))
					if ((x.getQuestIntId() > 0) && (x.getQuestIntId() < 20000))
						options.add(x);
			}
		}

		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if (options.size() > 1)
		{
			showQuestChooseWindow(player, options.toArray(new Quest[options.size()]));
		}
		else if (options.size() == 1)
		{
			showQuestWindow(player, options.get(0).getName());
		}
		else
		{
			showQuestWindow(player, "");
		}
	}

	/**
	 * Open a Loto window on client with the text of the L2NpcInstance.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR>
	 * 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 * 
	 */
	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public void showLotoWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().npcId;
		String filename;
		SystemMessage sm;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if (val == 0) // 0 - first buy lottery ticket window
		{
			filename = (getHtmlPath(npcId, 1));
			html.setFile(filename);
		}
		else if (val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if (!Lottery.getInstance().isStarted())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}

			filename = (getHtmlPath(npcId, 5));
			html.setFile(filename);

			int count = 0;
			int found = 0;
			// counting buttons and unsetting button if found
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == val)
				{
					//unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if (player.getLoto(i) > 0)
				{
					count++;
				}
			}

			//if not rearched limit 5 and not unseted value
			if (count < 5 && found == 0 && val <= 20)
				for (int i = 0; i < 5; i++)
					if (player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}

			//setting pusshed buttons
			count = 0;
			for (int i = 0; i < 5; i++)
				if (player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if (player.getLoto(i) < 10)
						button = "0" + button;
					String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}

			if (count == 5)
			{
				String search = "0\">Return";
				String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
		}
		else if (val == 22) //22 - selected ticket with 5 numbers
		{
			if (!Lottery.getInstance().isStarted())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}

			long price = Config.ALT_LOTTERY_TICKET_PRICE;
			int lotonumber = Lottery.getInstance().getId();
			int enchant = 0;
			int type2 = 0;

			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == 0)
					return;
				
				if (player.getLoto(i) < 17)
					enchant += Math.pow(2, player.getLoto(i) - 1);
				else
					type2 += Math.pow(2, player.getLoto(i) - 17);
			}
			if (player.getAdena() < price)
			{
				sm = new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				return;
			}
			if (!player.reduceAdena("Loto", price, this, true))
				return;
			Lottery.getInstance().increasePrize(price);

			sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
			sm.addNumber(lotonumber);
			sm.addItemName(4442);
			player.sendPacket(sm);

			L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
			item.setCount(1);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem("Loto", item, player, this);

			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
			iu.addModifiedItem(adenaupdate);
			player.sendPacket(iu);

			filename = (getHtmlPath(npcId, 3));
			html.setFile(filename);
		}
		else if (val == 23) //23 - current lottery jackpot
		{
			filename = (getHtmlPath(npcId, 3));
			html.setFile(filename);
		}
		else if (val == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = (getHtmlPath(npcId, 4));
			html.setFile(filename);

			int lotonumber = Lottery.getInstance().getId();
			String message = "";
			for (L2ItemInstance item : player.getInventory().getItems())
			{
				if (item == null)
					continue;
				if (item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					int[] numbers = Lottery.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for (int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					long[] check = Lottery.getInstance().checkTicket(item);
					if (check[0] > 0)
					{
						switch ((int)check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if (message.isEmpty())
			{
				message += "There is no winning lottery ticket...<br>";
			}
			html.replace("%result%", message);
		}
		else if (val > 24) // >24 - check lottery ticket by item object id
		{
			int lotonumber = Lottery.getInstance().getId();
			L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if (item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
				return;
			long[] check = Lottery.getInstance().checkTicket(item);

			sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(4442);
			sm.addItemNumber(1);
			player.sendPacket(sm);

			long adena = check[1];
			if (adena > 0)
				player.addAdena("Loto", adena, this, true);
			player.destroyItem("Loto", item, this, false);
			return;
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + Lottery.getInstance().getId());
		html.replace("%adena%", "" + Lottery.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + (Config.ALT_LOTTERY_5_NUMBER_RATE * 100));
		html.replace("%prize4%", "" + (Config.ALT_LOTTERY_4_NUMBER_RATE * 100));
		html.replace("%prize3%", "" + (Config.ALT_LOTTERY_3_NUMBER_RATE * 100));
		html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void makeCPRecovery(L2PcInstance player)
	{
		if (getNpcId() != 31225 && getNpcId() != 31226)
			return;
		if (player.isCursedWeaponEquipped())
		{
			player.sendMessage("Go away, you're not welcome here.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int neededmoney = 100;
		if (!player.reduceAdena("RestoreCP", neededmoney, player.getLastFolkNPC(), true))
			return;
		L2Skill skill = SkillTable.getInstance().getInfo(4380, 1);
		if (skill != null)
		{
			setTarget(player);
			doCast(skill);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Add Newbie helper buffs to L2Player according to its level.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the range level in wich player must be to obtain buff </li>
	 * <li>If player level is out of range, display a message and return </li>
	 * <li>According to player level cast buff </li><BR><BR>
	 * 
	 * <FONT COLOR=#FF0000><B> Newbie Helper Buff list is define in sql table helper_buff_list</B></FONT><BR><BR>
	 * 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * 
	 */
	public void makeSupportMagic(L2PcInstance player, boolean isSummon)
	{
		if (player == null)
			return;

		// Prevent a cursed weapon weilder of being buffed
		if (player.isCursedWeaponEquipped())
			return;

		int player_level = player.getLevel();
		int lowestLevel = 0;
		int highestLevel = 0;

		if (isSummon)
		{
			if (player.getPet() == null || !(player.getPet() instanceof L2SummonInstance))
			{
				String content = "<html><body>Only servitors can receive this Support Magic. If you do not have a servitor, you cannot access these spells.</body></html>";
				insertObjectIdAndShowChatWindow(player, content);
				return;
			}
			setTarget(player.getPet());
		}
		else
			// 	Select the player
			setTarget(player);

		if (isSummon)
		{
			lowestLevel = HelperBuffTable.getInstance().getServitorLowestLevel();
			highestLevel = HelperBuffTable.getInstance().getServitorHighestLevel();
		}
		else
		{
			// 	Calculate the min and max level between which the player must be to obtain buff
			if (player.isMageClass())
			{
				lowestLevel = HelperBuffTable.getInstance().getMagicClassLowestLevel();
				highestLevel = HelperBuffTable.getInstance().getMagicClassHighestLevel();
			}
			else
			{
				lowestLevel = HelperBuffTable.getInstance().getPhysicClassLowestLevel();
				highestLevel = HelperBuffTable.getInstance().getPhysicClassHighestLevel();
			}
		}
		// If the player is too high level, display a message and return
		if (player_level > highestLevel)
		{
			String content = "<html><body>Newbie Guide:<br>Only a <font color=\"LEVEL\">novice character of level " + highestLevel
					+ " or less</font> can receive my support magic.<br>Your novice character is the first one that you created and raised in this world.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		// If the player is too low level, display a message and return
		if (player_level < lowestLevel)
		{
			String content = "<html><body>Come back here when you have reached level " + lowestLevel + ". I will give you support magic then.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		L2Skill skill = null;
		if (isSummon)
		{
			for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
			{
				if (helperBuffItem.isForSummon())
				{
					skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
					if (skill != null)
						doCast(skill);
				}
			}
		}
		else
		{
			// 	Go through the Helper Buff list define in sql table helper_buff_list and cast skill
			for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
			{
				if (helperBuffItem.isMagicClassBuff() == player.isMageClass())
				{
					if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
					{
						skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
						if (skill.getSkillType() == L2SkillType.SUMMON)
							player.doSimultaneousCast(skill);
						else
							doCast(skill);
					}
				}
			}
		}
	}

	public void showChatWindow(L2PcInstance player)
	{
		showChatWindow(player, 0);
	}

	/**
	 * Returns true if html exists
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(L2PcInstance player, String type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-pk.htm");

		if (html != null)
		{
			NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(html);
			player.sendPacket(pkDenyMsg);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}

		return false;
	}

	/**
	 * Open a chat window on client with the text of the L2NpcInstance.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR>
	 * 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 * 
	 */
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (player.isCursedWeaponEquipped() && (!(player.getTarget() instanceof L2ClanHallManagerInstance) || !(player.getTarget() instanceof L2DoormenInstance)))
		{
			player.setTarget(player);
			return;
		}
		if (player.getKarma() > 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance)
			{
				if (showPkDenyChatWindow(player, "merchant"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance)
			{
				if (showPkDenyChatWindow(player, "teleporter"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof L2WarehouseInstance)
			{
				if (showPkDenyChatWindow(player, "warehouse"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance)
			{
				if (showPkDenyChatWindow(player, "fisherman"))
					return;
			}
		}

		if ("L2Auctioneer".equals(getTemplate().type) && val == 0)
			return;

		int npcId = getTemplate().npcId;

		/* For use with Seven Signs implementation */
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		int compWinner = SevenSigns.getInstance().getCabalHighestScore();

		switch (npcId)
		{
			case 31127: //
			case 31128: //
			case 31129: // Dawn Festival Guides
			case 31130: //
			case 31131: //
				filename += "festival/dawn_guide.htm";
				break;
			case 31137: //
			case 31138: //
			case 31139: // Dusk Festival Guides
			case 31140: //
			case 31141: //
				filename += "festival/dusk_guide.htm";
				break;
			case 31092: // Black Marketeer of Mammon
				filename += "blkmrkt_1.htm";
				break;
			case 31113: // Merchant of Mammon
				if (Config.ALT_STRICT_SEVENSIGNS)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN:
							if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK:
							if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						default:
							player.sendPacket(new SystemMessage(SystemMessageId.QUEST_EVENT_PERIOD));
							return;
					}
				}
				filename += "mammmerch_1.htm";
				break;
			case 31126: // Blacksmith of Mammon
				if (Config.ALT_STRICT_SEVENSIGNS)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN:
							if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK:
							if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						default:
							player.sendPacket(new SystemMessage(SystemMessageId.QUEST_EVENT_PERIOD));
							return;
					}
				}
				filename += "mammblack_1.htm";
				break;
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136: // Festival Witches
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
				filename += "festival/festival_witch.htm";
				break;
			case 31688:
				if (player.isNoble())
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				else
					filename = (getHtmlPath(npcId, val));
				break;
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isHero() || player.isNoble())
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				else
					filename = (getHtmlPath(npcId, val));
				break;
			case 36402:
				if (player.olyBuff > 0)
					filename = (player.olyBuff == 5 ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
				else
					filename = Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm";
				break;
			default:
				if (npcId >= 31865 && npcId <= 31918)
				{
					if (val == 0 )
						filename += "rift/GuardianOfBorder.htm";
					else
						filename += "rift/GuardianOfBorder-" + val + ".htm";
					break;
				}
				if ((npcId >= 31093 && npcId <= 31094) || (npcId >= 31172 && npcId <= 31201) || (npcId >= 31239 && npcId <= 31254))
					return;
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = (getHtmlPath(npcId, val));
				break;
		}

		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance 
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		if (this instanceof L2MerchantInstance)
		{
			if (Config.LIST_PET_RENT_NPC.contains(npcId))
				html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
		}

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Open a chat window on client with the text specified by the given file name and path,<BR>
	 * relative to the datapack root.
	 * <BR><BR>
	 * Added by Tempy 
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param filename The filename that contains the text to send
	 *  
	 */
	public void showChatWindow(L2PcInstance player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance 
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Return the Exp Reward of this L2NpcInstance contained in the L2NpcTemplate (modified by RATE_XP).<BR><BR>
	 */
	public int getExpReward()
	{
		double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		return (int) (getTemplate().rewardExp * rateXp * Config.RATE_XP);
	}

	/**
	 * Return the SP Reward of this L2NpcInstance contained in the L2NpcTemplate (modified by RATE_SP).<BR><BR>
	 */
	public int getSpReward()
	{
		double rateSp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		return (int) (getTemplate().rewardSp * rateSp * Config.RATE_SP);
	}

	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds).<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create a DecayTask to remove the corpse of the L2NpcInstance after 7 seconds </li>
	 * <li>Set target to null and cancel Attack or Cast </li>
	 * <li>Stop movement </li>
	 * <li>Stop HP/MP/CP Regeneration task </li>
	 * <li>Stop all active skills effects in progress on the L2Character </li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform </li>
	 * <li>Notify L2Character AI </li><BR><BR>
	 * 
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2Attackable </li><BR><BR>
	 * 
	 * @param killer The L2Character who killed it
	 * 
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		// normally this wouldn't really be needed, but for those few exceptions, 
		// we do need to reset the weapons back to the initial templated weapon.
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	/**
	 * Set the spawn of the L2NpcInstance.<BR><BR>
	 * 
	 * @param spawn The L2Spawn that manage the L2NpcInstance
	 * 
	 */
	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if (getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN) != null)
			for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
				quest.notifySpawn(this);
	}

	/**
	 * Remove the L2NpcInstance from the world and update its spawn object (for a complete removal use the deleteMe method).<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2NpcInstance from the world when the decay task is launched </li>
	 * <li>Decrease its spawn counter </li>
	 * <li>Manage Siege task (killFlag, killCT) </li><BR><BR>
	 * 
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR><BR>
	 * 
	 */
	@Override
	public void onDecay()
	{
		if (isDecayed())
			return;
		setDecayed(true);

		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();

		// Decrease its spawn counter
		if (_spawn != null)
			_spawn.decreaseCount(this);
	}

	/**
	 * Remove PROPERLY the L2NpcInstance from the world.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2NpcInstance from the world and update its spawn object </li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2NpcInstance then cancel Attack or Cast and notify AI </li>
	 * <li>Remove L2Object object from _allObjects of L2World </li><BR><BR>
	 * 
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR><BR>
	 * 
	 */
	public void deleteMe()
	{
		L2WorldRegion oldRegion = getWorldRegion();

		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed decayMe().", e);
		}
		try
		{
			if (_fusionSkill != null)
				abortCast();

			for (L2Character character : getKnownList().getKnownCharacters())
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		if (oldRegion != null)
			oldRegion.removeFromZones(this);

		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed removing cleaning knownlist.", e);
		}

		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
	}

	/**
	 * Return the L2Spawn object that manage this L2NpcInstance.<BR><BR>
	 */
	public L2Spawn getSpawn()
	{
		return _spawn;
	}

	@Override
	public String toString()
	{
		return getTemplate().name;
	}

	public boolean isDecayed()
	{
		return _isDecayed;
	}

	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}

	public void endDecayTask()
	{
		if (!isDecayed())
		{
			DecayTaskManager.getInstance().cancelDecayTask(this);
			onDecay();
		}
	}

	public boolean isMob() // rather delete this check
	{
		return false; // This means we use MAX_NPC_ANIMATION instead of MAX_MONSTER_ANIMATION
	}

	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		updateAbnormalEffect();
	}

	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		updateAbnormalEffect();
	}

	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		updateAbnormalEffect();
	}
	
	public void setEnchant(int newEnchantValue)
	{
		_currentEnchant = newEnchantValue;
		updateAbnormalEffect();
	}

	public void setCollisionHeight(int height)
	{
		_currentCollisionHeight = height;
	}

	public void setCollisionRadius(int radius)
	{
		_currentCollisionRadius = radius;
	}

	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (Config.CHECK_KNOWN)
			activeChar.sendMessage("Added NPC: "+getName());

		if (getRunSpeed() == 0)
			activeChar.sendPacket(new ServerObjectInfo(this, activeChar));
		else
			activeChar.sendPacket(new AbstractNpcInfo.NpcInfo(this, activeChar));
	}

	public void showNoTeachHtml(L2PcInstance player)
	{
		int npcId = getNpcId();
		String html = "";

		if (this instanceof L2WarehouseInstance)
			html = HtmCache.getInstance().getHtm("data/html/warehouse/" + npcId + "-noteach.htm");
		else if (this instanceof L2TrainerInstance)
			html = HtmCache.getInstance().getHtm("data/html/trainer/" + npcId + "-noteach.htm");

		if (html == null)
		{
			_log.warning("Npc " + npcId + " missing noTeach html!");
			NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
			final String sb = StringUtil.concat(
					"<html><body>" +
					"I cannot teach you any skills.<br>You must find your current class teachers.",
					"</body></html>"
			);
			msg.setHtml(sb);
			player.sendPacket(msg);
			return;
		}
		else
		{
			NpcHtmlMessage noTeachMsg = new NpcHtmlMessage(getObjectId());
			noTeachMsg.setHtml(html);
			noTeachMsg.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(noTeachMsg);
		}
	}
	
	public void scheduleDespawn(long delay)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(this.new DespawnTask(this), delay);
	}
	
	public class DespawnTask implements Runnable
	{
		L2Npc _npc;
		
		public DespawnTask(L2Npc npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			if (_npc != null)
				_npc.deleteMe();
		}		
	}
}