package net.sf.l2j.gameserver.model.actor.poly;

import net.sf.l2j.gameserver.model.L2Object;

public class ObjectPoly
{
    // =========================================================
    // Data Field
    private L2Object _ActiveObject;          
    private int _PolyId;
    private String _PolyType;
    
    // =========================================================
    // Constructor
    public ObjectPoly(L2Object activeObject)
    {
        _ActiveObject = activeObject;
    }
    
    // =========================================================
    // Method - Public
    public void setPolyInfo(String polyType, String polyId)
    {
        setPolyId(Integer.parseInt(polyId));     
        setPolyType(polyType);
    }
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public final L2Object getActiveObject()
    {
        return _ActiveObject;
    }
    
    public final boolean isMorphed() { return getPolyType() != null; }
    
    public final int getPolyId() { return _PolyId; }
    public final void setPolyId(int value) { _PolyId = value; }
    
    public final String getPolyType() { return _PolyType; }
    public final void setPolyType(String value) { _PolyType = value; }
}
