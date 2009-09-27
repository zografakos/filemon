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
package net.sf.l2j.gameserver.skills.l2skills;

import java.util.logging.Level;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class L2SkillTeleport extends L2Skill
{
	private final String _recallType;
	private final int[] _teleportCoords;

	public L2SkillTeleport(StatsSet set)
	{
		super(set);

		_recallType = set.getString("recallType", "");
		String coords = set.getString("teleCoords", null);
		if (coords != null)
		{
			String[] valuesSplit = coords.split(",");
			_teleportCoords = new int[valuesSplit.length];
			for (int i = 0; i < valuesSplit.length;i++)
				_teleportCoords[i] = Integer.parseInt(valuesSplit[i]);
		}
		else
			_teleportCoords = null;
	}

	@Override
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if (activeChar instanceof L2PcInstance)
		{
			// Thanks nbd
			if (!TvTEvent.onEscapeUse(((L2PcInstance) activeChar).getObjectId()))
			{
				((L2PcInstance) activeChar).sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (((L2PcInstance) activeChar).isInOlympiadMode())
			{
				((L2PcInstance) activeChar).sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
				return;
			}

			if (GrandBossManager.getInstance().getZone(activeChar) != null && !activeChar.isGM())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
				return;
			}
		}

		try
		{
			for (L2Character target: (L2Character[]) targets)
			{
				if (target instanceof L2PcInstance)
				{
					L2PcInstance targetChar = (L2PcInstance) target;

					// Check to see if the current player target is in a festival.
					if (targetChar.isFestivalParticipant())
					{
						targetChar.sendMessage("You may not use an escape skill in a festival.");
						continue;
					}

					// Check to see if player is in jail
					if (targetChar.isInJail())
					{
						targetChar.sendMessage("You can not escape from jail.");
						continue;
					}

					// Check to see if player is in a duel
					if (targetChar.isInDuel())
					{
						targetChar.sendMessage("You cannot use escape skills during a duel.");
						continue;
					}
				}
				target.setInstanceId(0);
				if (target instanceof L2PcInstance)
					((L2PcInstance)target).setIsIn7sDungeon(false);

				if (getSkillType() == L2SkillType.TELEPORT)
				{
					if (_teleportCoords != null)
					{
						if (activeChar instanceof L2PcInstance && !((L2PcInstance) activeChar).isFlyingMounted())
							target.teleToLocation(_teleportCoords[0], _teleportCoords[1], _teleportCoords[2]);
					}
				}
				else
				{
					if (_recallType.equalsIgnoreCase("Castle"))
					{
						if (activeChar instanceof L2PcInstance && !((L2PcInstance) activeChar).isFlyingMounted())
							target.teleToLocation(MapRegionTable.TeleportWhereType.Castle);
					}
					else if (_recallType.equalsIgnoreCase("ClanHall"))
					{
						if (activeChar instanceof L2PcInstance && !((L2PcInstance) activeChar).isFlyingMounted())
							target.teleToLocation(MapRegionTable.TeleportWhereType.ClanHall);
					}
					else if (_recallType.equalsIgnoreCase("Fortress"))
					{
						if (activeChar instanceof L2PcInstance && !((L2PcInstance) activeChar).isFlyingMounted())
							target.teleToLocation(MapRegionTable.TeleportWhereType.Fortress);
					}
					else
						target.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
}