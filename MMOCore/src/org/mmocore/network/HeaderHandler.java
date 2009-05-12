/* This program is free software; you can redistribute it and/or modify
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
package org.mmocore.network;

/**
 * @author KenM
 *
 */
public abstract class HeaderHandler<T extends MMOClient, H extends HeaderHandler<T,H>>
{
    private final H _subHeaderHandler;
    
    public HeaderHandler(H subHeaderHandler)
    {
        _subHeaderHandler = subHeaderHandler;
    }

    /**
     * @return the subHeaderHandler
     */
    public final H getSubHeaderHandler()
    {
        return _subHeaderHandler;
    }
    
    public final boolean isChildHeaderHandler()
    {
        return this.getSubHeaderHandler() == null;
    }
}