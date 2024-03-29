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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExPutEnchantSupportItemResult;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 *
 * @author  KenM
 */
public class RequestExTryToPutEnchantSupportItem extends AbstractEnchantPacket
{
	
	private int _supportObjectId;
	private int _enchantObjectId;
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] D0:50 RequestExTryToPutEnchantSupportItem".intern();
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
	 */
	@Override
	protected void readImpl()
	{
		_supportObjectId = readD();
		_enchantObjectId = readD();
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = this.getClient().getActiveChar();
		if (activeChar != null)
		{
			if (activeChar.isEnchanting())
			{
				L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_enchantObjectId);
				L2ItemInstance support = activeChar.getInventory().getItemByObjectId(_supportObjectId);
				
				if (item == null || support == null)
					return;
				
				EnchantItem supportTemplate = getSupportItem(support);
				
				if (supportTemplate == null || !supportTemplate.isValid(item))
				{
					// message may be custom
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
					activeChar.setActiveEnchantSupportItem(null);
					activeChar.sendPacket(new ExPutEnchantSupportItemResult(0));
					return;
				}
				activeChar.setActiveEnchantSupportItem(support);
				activeChar.sendPacket(new ExPutEnchantSupportItemResult(_supportObjectId));
			}
		}
	}
	
}
