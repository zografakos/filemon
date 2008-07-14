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
package net.sf.l2j.gameserver.taskmanager;

import java.util.Collection;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;

public class KnownListUpdateTaskManager
{
	protected static final Logger _log = Logger.getLogger(DecayTaskManager.class.getName());
	
	private Object syncObject = new Object();
	
	private static KnownListUpdateTaskManager _instance;
	
	public KnownListUpdateTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAi(new KnownListUpdate(), 1000);
	}
	
	public static KnownListUpdateTaskManager getInstance()
	{
		if (_instance == null)
			_instance = new KnownListUpdateTaskManager();
		
		return _instance;
	}
	
	public Object getSync()
	{
		return syncObject;
	}
	
	private class KnownListUpdate implements Runnable
	{
		public KnownListUpdate()
		{
		}
		
		public void run()
		{
			try
			{
				for (L2WorldRegion regions[] : L2World.getInstance().getAllWorldRegions())
				{
					for (L2WorldRegion r : regions) // go through all world
					// regions
					{
						// avoid stopping update if something went wrong in updateRegion()
						try
						{
							if (r.isActive()) // and check only if the region
							// is active
							{
								updateRegion(r, true, true);
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			catch (Throwable e)
			{
				_log.warning(e.toString());
			}
			ThreadPoolManager.getInstance().scheduleAi(new KnownListUpdate(), Config.KNOWNLIST_UPDATE_INTERVAL);
		}
	}
	
	public void updateRegion(L2WorldRegion region, boolean fullUpdate,
	        boolean forgetObjects)
	{
		synchronized (syncObject)
		{
			Collection<L2Object> vObj = region.getVisibleObjects().values();
			synchronized (region.getVisibleObjects())
			{
				for (L2Object object : vObj) // and for all members in region
				{
					if (!object.isVisible())
						continue; // skip dying objects
					if (forgetObjects)
					{
						object.getKnownList().forgetObjects((object instanceof L2PlayableInstance
						        || (Config.GUARD_ATTACK_AGGRO_MOB && object instanceof L2GuardInstance) || fullUpdate));
					}
					if (object instanceof L2PlayableInstance
					        || (Config.GUARD_ATTACK_AGGRO_MOB && object instanceof L2GuardInstance)
					        || fullUpdate)
					{
						for (L2WorldRegion regi : region.getSurroundingRegions()) 
						{
							Collection<L2Object> inrObj = regi.getVisibleObjects().values();
							synchronized (regi.getVisibleObjects())
							{
								for (L2Object _object : inrObj)
									if (_object != object)
										object.getKnownList().addKnownObject(_object);
							}
						}
					}
					else if (object instanceof L2Character)
						for (L2WorldRegion regi : region.getSurroundingRegions())
						{
							Collection<L2PlayableInstance> inrPls = regi.getVisiblePlayable().values();
							synchronized (regi.getVisiblePlayable())
							{
								if (regi.isActive())
									for (L2Object _object : inrPls)
										if (_object != object)
											object.getKnownList().addKnownObject(_object);
							}
						}
				}
			}
		}
	}
}
