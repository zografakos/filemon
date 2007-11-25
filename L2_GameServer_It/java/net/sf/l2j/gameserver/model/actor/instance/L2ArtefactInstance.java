/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class manages all Castle Siege Artefacts.<BR><BR>
 *
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/06 16:13:40 $
 */
public final class L2ArtefactInstance extends L2NpcInstance
{
    //private static Logger _log = Logger.getLogger(L2GuardInstance.class.getName());

	/**
	 * Constructor of L2ArtefactInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2ArtefactInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2ArtefactInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param L2NpcTemplate Template to apply to the NPC
	 */
    public L2ArtefactInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

	/**
	 * Return False.<BR><BR>
	 */
    @Override
	@SuppressWarnings("unused")
    public boolean isAutoAttackable(L2Character attacker)
	{
        return false;
    }

    @Override
	public boolean isAttackable()
    {
        return false;
    }


	/**
	 * Manage actions when a player click on the L2ArtefactInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client </li><BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : Action, AttackRequest</li><BR><BR>
	 *
	 * @param player The L2PcInstance that start an action on the L2ArtefactInstance
	 *
	 */
    @Override
	public void onAction(L2PcInstance player)
    {
        if (getObjectId() != player.getTargetId())
        {
			// Set the player AI Intention to AI_INTENTION_IDLE
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);

			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The color to display in the select window is White
            MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
            player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2ArtefactInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        }
    }

    @Override
	public void reduceCurrentHp(double damage, L2Character attacker){}
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake){}

}
