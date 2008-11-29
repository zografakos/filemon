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
package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;

public class FolkStatus extends NpcStatus
{
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public FolkStatus(L2NpcInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public final void reduceHp(double value, L2Character attacker) { reduceHp(value, attacker, true, false); }

    @Override
	public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT)
    {
        return;
    }
    
    @Override
    public final void reduceMp(double value)
    {
    	return;
    }

    @Override
	public L2FolkInstance getActiveChar() { return (L2FolkInstance)super.getActiveChar(); }
}
