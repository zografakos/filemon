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
package com.l2jserver.gameserver.skills.l2skills;

import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.templates.StatsSet;

public class L2SkillSiegeFlag extends L2Skill
{
	private final boolean _isAdvanced;
	
	public L2SkillSiegeFlag(StatsSet set)
	{
		super(set);
		_isAdvanced = set.getBool("isadvanced", false);
	}

	/**
	 * @see com.l2jserver.gameserver.model.L2Skill#useSkill(com.l2jserver.gameserver.model.actor.L2Character, com.l2jserver.gameserver.model.L2Object[])
	 */
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId())
			return;
		
		Castle castle = CastleManager.getInstance().getCastle(player);
		Fort fort = FortManager.getInstance().getFort(player);
		
		if ((castle == null) && (fort == null))
			return;
		
		if (castle != null)
		{
			if (!checkIfOkToPlaceFlag(player, castle, true))
				return;
		}
		else
		{
			if (!checkIfOkToPlaceFlag(player, fort, true))
				return;
		}
		
		try
		{
			// Spawn a new flag
			L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(35062), _isAdvanced);
			flag.setTitle(player.getClan().getName());
			flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
			flag.setHeading(player.getHeading());
			flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
			if (castle != null)
				castle.getSiege().getFlag(player.getClan()).add(flag);
			else
				fort.getSiege().getFlag(player.getClan()).add(flag);
			
		}
		catch (Exception e)
		{
			player.sendMessage("Error placing flag:" + e);
		}
	}
	
	/**
	 * Return true if character clan place a flag<BR><BR>
	 *
	 * @param activeChar The L2Character of the character placing the flag
	 * @param isCheckOnly if false, it will send a notification to the player telling him
	 * why it failed
	 */
	public static boolean checkIfOkToPlaceFlag(L2Character activeChar, boolean isCheckOnly)
	{
		Castle castle = CastleManager.getInstance().getCastle(activeChar);
		Fort fort = FortManager.getInstance().getFort(activeChar);
		
		if ((castle == null) && (fort == null))
			return false;
		if (castle != null)
			return checkIfOkToPlaceFlag(activeChar, castle, isCheckOnly);
		else
			return checkIfOkToPlaceFlag(activeChar, fort, isCheckOnly);
	}
	
	/**
	 * 
	 * @param activeChar
	 * @param castle
	 * @param isCheckOnly
	 * @return
	 */
	public static boolean checkIfOkToPlaceFlag(L2Character activeChar, Castle castle, boolean isCheckOnly)
	{
		if (!(activeChar instanceof L2PcInstance))
			return false;
		
		String text = "";
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (castle == null || castle.getCastleId() <= 0)
			text = "You must be on castle ground to place a flag.";
		else if (!castle.getSiege().getIsInProgress())
			text = "You can only place a flag during a siege.";
		else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
			text = "You must be an attacker to place a flag.";
		else if (player.getClan() == null || !player.isClanLeader())
			text = "You must be a clan leader to place a flag.";
		else if (castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
			text = "You have already placed the maximum number of flags possible.";
		else if (player.isInsideZone(L2Character.ZONE_NOHQ))
			text = "You cannot place flag here.";
		else
			return true;
		
		if (!isCheckOnly)
			player.sendMessage(text);
		return false;
	}
	
	/**
	 * 
	 * @param activeChar
	 * @param fort
	 * @param isCheckOnly
	 * @return
	 */
	public static boolean checkIfOkToPlaceFlag(L2Character activeChar, Fort fort, boolean isCheckOnly)
	{
		if (!(activeChar instanceof L2PcInstance))
			return false;
		
		String text = "";
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (fort == null || fort.getFortId() <= 0)
			text = "You must be on fort ground to place a flag.";
		else if (!fort.getSiege().getIsInProgress())
			text = "You can only place a flag during a siege.";
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
			text = "You must be an attacker to place a flag.";
		else if (player.getClan() == null || !player.isClanLeader())
			text = "You must be a clan leader to place a flag.";
		else if (fort.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= FortSiegeManager.getInstance().getFlagMaxCount())
			text = "You have already placed the maximum number of flags possible.";
		else if (player.isInsideZone(L2Character.ZONE_NOHQ))
			text = "You cannot place flag here.";
		else
			return true;
		
		if (!isCheckOnly)
			player.sendMessage(text);
		return false;
	}

}