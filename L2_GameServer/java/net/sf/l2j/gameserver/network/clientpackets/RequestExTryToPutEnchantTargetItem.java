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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutEnchantTargetItemResult;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

/**
 *
 * @author  KenM
 */
public class RequestExTryToPutEnchantTargetItem extends L2GameClientPacket
{

	private int _objectId = 0;

	/**
	 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] D0:4F RequestExTryToPutEnchantTargetItem";
	}

	/**
	 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
	 */
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	/**
	 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (_objectId == 0)
			return;

		if (activeChar != null)
		{
			if (activeChar.isEnchanting())
				return;

			L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_objectId);
			L2ItemInstance enchantScroll = activeChar.getActiveEnchantItem();

			if (targetItem == null || enchantScroll == null)
				return;

			activeChar.setIsEnchanting(true);

			switch (targetItem.getLocation())
			{
				case INVENTORY:
				case PAPERDOLL:
					// can't enchant rods, hero weapons, adventurers' items,shadow and common items
					if (targetItem.getOwnerId() != activeChar.getObjectId()
							|| targetItem.getItem().getItemType() == L2WeaponType.ROD
							|| targetItem.isHeroItem()
							|| (targetItem.getItemId() >= 7816 && targetItem.getItemId() <= 7831)
							|| targetItem.isShadowItem()
							|| targetItem.isCommonItem()
							|| targetItem.isTimeLimitedItem()
							|| targetItem.isEtcItem()
							|| targetItem.isWear()
							|| targetItem.getItem().getBodyPart() == L2Item.SLOT_L_BRACELET
							|| targetItem.getItem().getBodyPart() == L2Item.SLOT_R_BRACELET
							|| (targetItem.getLocation() != L2ItemInstance.ItemLocation.INVENTORY && targetItem.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.DOES_NOT_FIT_SCROLL_CONDITIONS));
						activeChar.setActiveEnchantItem(null);
						activeChar.sendPacket(new ExPutEnchantTargetItemResult(0));
						return;
					}
			}

			int itemType2 = targetItem.getItem().getType2();
			boolean enchantItem = false;

			/** pretty code ;D */
			switch (targetItem.getItem().getCrystalType())
			{
				case L2Item.CRYSTAL_A:
					switch (enchantScroll.getItemId())
					{
						case 729:
						case 731:
						case 6569:
						case 22009:
						case 22015:
						case 22019:
							if (itemType2 == L2Item.TYPE2_WEAPON)
								enchantItem = true;
							break;
						case 730:
						case 732:
						case 6570:
						case 22013:
						case 22017:
						case 22021:
							if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
								enchantItem = true;
							break;
					}
					break;
				case L2Item.CRYSTAL_B:
					switch (enchantScroll.getItemId())
					{
						case 947:
						case 949:
						case 6571:
						case 22008:
						case 22014:
						case 22018:
							if (itemType2 == L2Item.TYPE2_WEAPON)
								enchantItem = true;
							break;
						case 948:
						case 950:
						case 6572:
						case 22012:
						case 22016:
						case 22020:
							if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
								enchantItem = true;
							break;
					}
					break;
				case L2Item.CRYSTAL_C:
					switch (enchantScroll.getItemId())
					{
						case 951:
						case 953:
						case 6573:
						case 22007:
							if (itemType2 == L2Item.TYPE2_WEAPON)
								enchantItem = true;
							break;
						case 952:
						case 954:
						case 6574:
						case 22011:
							if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
								enchantItem = true;
							break;
					}
					break;
				case L2Item.CRYSTAL_D:
					switch (enchantScroll.getItemId())
					{
						case 955:
						case 957:
						case 6575:
						case 22006:
							if (itemType2 == L2Item.TYPE2_WEAPON)
								enchantItem = true;
							break;
						case 956:
						case 958:
						case 6576:
						case 22010:
							if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
								enchantItem = true;
							break;
					}
					break;
				case L2Item.CRYSTAL_S:
				case L2Item.CRYSTAL_S80:
				case L2Item.CRYSTAL_S84:
					switch (enchantScroll.getItemId())
					{
						case 959:
						case 961:
						case 6577:
							if (itemType2 == L2Item.TYPE2_WEAPON)
								enchantItem = true;
							break;
						case 960:
						case 962:
						case 6578:
							if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
								enchantItem = true;
							break;
					}
					break;
			}

			int maxEnchantLevel = 0;
			switch (itemType2)
			{
				case L2Item.TYPE2_WEAPON: 
					maxEnchantLevel = Config.ENCHANT_MAX_WEAPON;
					break;
				case L2Item.TYPE2_SHIELD_ARMOR:
					maxEnchantLevel = Config.ENCHANT_MAX_ARMOR;
					break;
				case L2Item.TYPE2_ACCESSORY:
					maxEnchantLevel = Config.ENCHANT_MAX_JEWELRY;
					break;
			}

			if (maxEnchantLevel != 0 && targetItem.getEnchantLevel() >= maxEnchantLevel)
				enchantItem = false;

			// Ancient enchant crystals can enchant only up to 16
			if (targetItem.getEnchantLevel() >= 16 && enchantScroll.getItemId() >= 22014 && enchantScroll.getItemId() <= 22017)
				enchantItem = false;

			if (!enchantItem)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.DOES_NOT_FIT_SCROLL_CONDITIONS));
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(new ExPutEnchantTargetItemResult(0));
				return;
			}
			activeChar.setActiveEnchantTimestamp(System.currentTimeMillis());
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(_objectId));
		}
	}
}
