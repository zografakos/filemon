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
package net.sf.l2j.gameserver.clientpackets;

import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ExEnchantSkillInfo;
import net.sf.l2j.gameserver.serverpackets.ExEnchantSkillList.EnchantSkillType;

/**
 * Format (ch) dd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public final class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private static final Logger _log = Logger.getLogger(RequestExEnchantSkillInfo.class.getName());
	private static final String _C__D0_06_REQUESTEXENCHANTSKILLINFO = "[C] D0:06 RequestExEnchantSkillInfo";

    private static final int TYPE_NORMAL_ENCHANT = 0;
    private static final int TYPE_SAFE_ENCHANT = 1;
    private static final int TYPE_UNTRAIN_ENCHANT = 2;
    private static final int TYPE_CHANGE_ENCHANT = 3;
    
    private int _type;
	private int _skillId;
	private int _skillLvl;

	@Override
	protected void readImpl()
	{
        _type = readD();
		_skillId = readD();
		_skillLvl = readD();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

        if (activeChar == null)
            return;

        if (activeChar.getLevel() < 76)
            return;

        L2FolkInstance trainer = activeChar.getLastFolkNPC();

        if ((trainer == null || !activeChar.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !activeChar.isGM())
            return;

        L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
        
        if (skill == null || skill.getId() != _skillId)
        {
        	activeChar.sendMessage("This skill doesn't yet have enchant info in Datapack");
            return;
        }

        if (!trainer.getTemplate().canTeach(activeChar.getClassId()) && !activeChar.isGM())
        	return; // cheater
        
        switch (_type)
        {
            case TYPE_NORMAL_ENCHANT:
                this.showEnchantInfo(activeChar, false);
                break;
            case TYPE_SAFE_ENCHANT:
                this.showEnchantInfo(activeChar, true);
                break;
            case TYPE_UNTRAIN_ENCHANT:
                this.showUntrainEnchantInfo(activeChar);
                break;
            case TYPE_CHANGE_ENCHANT:
                this.showChangeEnchantInfo(activeChar);
                break; 
            default:
                _log.severe("Unknown skill enchant type: "+_type);
                break;
        }
	}
    
    public void showEnchantInfo(L2PcInstance activeChar, boolean isSafeEnchant)
    {
        ExEnchantSkillInfo asi = new ExEnchantSkillInfo(isSafeEnchant ? EnchantSkillType.SAFE : EnchantSkillType.NORMAL, _skillId);
        
        L2EnchantSkillLearn enchantLearn = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
        // do we have this skill?
        if (enchantLearn != null)
        {
            // skill already enchanted?
            if (_skillLvl > 100)
            {
                // get detail for next level
                EnchantSkillDetail esd = enchantLearn.getEnchantSkillDetail(_skillLvl + 1);
                
                // if it exists add it
                if (esd != null)
                {
                    asi.addEnchantSkillDetail(activeChar, esd);
                }
            }
            else // not already enchanted
            {
                for (List<EnchantSkillDetail> esd : enchantLearn.getEnchantRoutes())
                {
                    // add first level (+1) of all routes
                    asi.addEnchantSkillDetail(activeChar, esd.get(0));
                }
            }
            sendPacket(asi);
        }
    }
    
    public void showChangeEnchantInfo(L2PcInstance activeChar)
    {
        ExEnchantSkillInfo asi = new ExEnchantSkillInfo(EnchantSkillType.CHANGE_ROUTE, _skillId);
        
        L2EnchantSkillLearn enchantLearn = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
        // do we have this skill?
        if (enchantLearn != null)
        {
            // skill already enchanted?
            if (_skillLvl > 100)
            {
                // get current enchant type
                int currentType = L2EnchantSkillLearn.getEnchantType(_skillLvl);
                
                List<EnchantSkillDetail>[] routes = enchantLearn.getEnchantRoutes();
                List<EnchantSkillDetail> route;
                for (int i = 0; i < routes.length; i++)
                {
                    // skip current route
                    if (i != currentType)
                    {
                        route = routes[i];
                        EnchantSkillDetail esd = route.get(L2EnchantSkillLearn.getEnchantIndex(_skillLvl));
                        if (esd != null)
                        {
                            asi.addEnchantSkillDetail(activeChar, esd);
                        }
                    }
                }
                
                
                sendPacket(asi);
            }
            else
            {
                _log.warning("Client: "+this.getClient()+" requested change route information for unenchanted skill");
            }
        }
    }
    
    public void showUntrainEnchantInfo(L2PcInstance activeChar)
    {
        ExEnchantSkillInfo asi = new ExEnchantSkillInfo(EnchantSkillType.UNTRAIN, _skillId);
        
        L2EnchantSkillLearn enchantLearn = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
        // do we have this skill?
        if (enchantLearn != null)
        {
            // skill already enchanted?
            if (_skillLvl > 100)
            {
                EnchantSkillDetail currentLevelDetail = enchantLearn.getEnchantSkillDetail(_skillLvl);
                if (currentLevelDetail != null)
                {
                    // no previous enchant level, return to original
                    if (_skillLvl%100 == 1)
                    {
                        asi.addEnchantSkillDetail(enchantLearn.getBaseLevel(), 100, currentLevelDetail.getSpCost(), currentLevelDetail.getExp());
                    }
                    else
                    {
                        // get detail for previous level
                        EnchantSkillDetail esd = enchantLearn.getEnchantSkillDetail(_skillLvl - 1);
                        
                        // if it exists add it
                        if (esd != null)
                        {
                            asi.addEnchantSkillDetail(esd.getLevel(), 100, currentLevelDetail.getSpCost(), currentLevelDetail.getExp());
                        }
                    }
                }
                else
                {
                    _log.warning("Client: "+this.getClient()+" tried to untrain enchanted skill, but server doesnt has data for his current skill enchantment level");
                }
                
                sendPacket(asi);
            }
            else
            {
                _log.warning("Client: "+this.getClient()+" requested untrain information for unenchanted skill");
            }
        }
    }

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_06_REQUESTEXENCHANTSKILLINFO;
	}

}