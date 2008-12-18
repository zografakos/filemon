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
package net.sf.l2j.gameserver.templates.item;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * This class is dedicated to the management of armors.
 *
 * @version $Revision: 1.2.2.1.2.6 $ $Date: 2005/03/27 15:30:10 $
 */
public final class L2Armor extends L2Item
{
	private final int _avoidModifier;
	private final int _pDef;
	private final int _mDef;
	private final int _mpBonus;
	private final int _hpBonus;
	private final String[] _skill;

    /**
     * Constructor for Armor.<BR><BR>
     * <U><I>Variables filled :</I></U><BR>
     * <LI>_avoidModifier</LI>
     * <LI>_pDef & _mDef</LI>
     * <LI>_mpBonus & _hpBonus</LI>
     * @param type : L2ArmorType designating the type of armor
     * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
     * @see L2Item constructor
     */
	public L2Armor(L2ArmorType type, StatsSet set)
	{
		super(type, set);
		_avoidModifier = set.getInteger("avoid_modify");
		_pDef          = set.getInteger("p_def");
		_mDef          = set.getInteger("m_def");
		_mpBonus       = set.getInteger("mp_bonus", 0);
		_hpBonus       = set.getInteger("hp_bonus", 0);

		_skill = set.getString("skill").split(";");
	}

	/**
	 * Returns the type of the armor.
	 * @return L2ArmorType
	 */
	@Override
	public L2ArmorType getItemType()
	{
		return (L2ArmorType)super._type;
	}

	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}

	/**
	 * Returns the magical defense of the armor
	 * @return int : value of the magic defense
	 */
	public final int getMDef()
	{
		return _mDef;
	}

	/**
	 * Returns the physical defense of the armor
	 * @return int : value of the physical defense
	 */
	public final int getPDef()
	{
		return _pDef;
	}

	/**
	 * Returns avoid modifier given by the armor
	 * @return int : avoid modifier
	 */
	public final int getAvoidModifier()
	{
		return _avoidModifier;
	}

	/**
	 * Returns magical bonus given by the armor
	 * @return int : value of the magical bonus
	 */
	public final int getMpBonus()
	{
		return _mpBonus;
	}

	/**
	 * Returns physical bonus given by the armor
	 * @return int : value of the physical bonus
	 */
	public final int getHpBonus()
	{
		return _hpBonus;
	}

	/**
	 * Returns passive skill linked to that armor
	 * @return
	 */
	public String[] getSkills()
	{
		return _skill;
	}

	/**
	 * Returns array of Func objects containing the list of functions used by the armor
	 * @param instance : L2ItemInstance pointing out the armor
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
    {
    	List<Func> funcs = new FastList<Func>();
    	if (_funcTemplates != null)
    	{
    		for (FuncTemplate t : _funcTemplates) {
		    	Env env = new Env();
		    	env.player = player;
		    	env.item = instance;
		    	Func f = t.getFunc(env, instance);
		    	if (f != null)
			    	funcs.add(f);
    		}
    	}
    	return funcs.toArray(new Func[funcs.size()]);
    }
}