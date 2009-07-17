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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2FortWyvernManagerInstance extends L2WyvernManagerInstance
{
	public L2FortWyvernManagerInstance (int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected final boolean isOwnerClan(L2PcInstance player)
	{
		if (player.getClan() != null && getFort() != null && getFort().getOwnerClan() != null)
		{
			if (player.getClanId() == getFort().getOwnerClan().getClanId() && player.isClanLeader())
				return true;
		}
		return false;
	}
}
