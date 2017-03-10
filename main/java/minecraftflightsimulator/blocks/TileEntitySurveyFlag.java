package minecraftflightsimulator.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecraftflightsimulator.packets.general.TileEntitySyncPacket;
import minecraftflightsimulator.utilites.MFSCurve;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntitySurveyFlag extends TileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public float angle;
	public MFSCurve linkedCurve;
		
	public TileEntitySurveyFlag(){
		super();
	}
	
	public TileEntitySurveyFlag(float angle){
		this.angle = angle;
	}
	
	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MFS.MFSNet.sendToServer(new TileEntityClientRequestDataPacket(this));
        }
    }
	
	@Override
	public void updateEntity(){
		super.updateEntity();
	}
	
	public void linkToFlag(int[] linkedFlagCoords, boolean isPrimary){
		if(linkedCurve != null){
			((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedCurve.blockEndPoint[0], linkedCurve.blockEndPoint[1], linkedCurve.blockEndPoint[2])).clearFlagLinking();
		}
		this.isPrimary = isPrimary;
		TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]));
		linkedCurve = new MFSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, linkedFlagCoords, angle, linkedFlag.angle);
		MFS.MFSNet.sendToAll(new TileEntitySyncPacket(this));
	}
	
	public void clearFlagLinking(){
		if(linkedCurve != null){
			int[] linkedFlagCoords = linkedCurve.blockEndPoint;
			linkedCurve = null;
			TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]));
			if(linkedFlag != null){
				linkedFlag.clearFlagLinking();
			}
			MFS.MFSNet.sendToAll(new TileEntitySyncPacket(this));
		}
	}
	
	public int[] setDummyTracks(){
		float[] currentPoint;		
		float currentAngle;
		float currentSin;
		float currentCos;
		
		//First make sure blocks can be placed.
		List<int[]> blockList = new ArrayList<int[]>();
		for(short i=0; i <= linkedCurve.pathLength; ++i){
			currentPoint = linkedCurve.getPointAt(i/linkedCurve.pathLength);
			currentAngle = 90 + linkedCurve.getYawAngleAt(i/linkedCurve.pathLength);
			currentSin = (float) Math.sin(Math.toRadians(currentAngle));
			currentCos = (float) Math.cos(Math.toRadians(currentAngle));

			int[] offset = new int[3];
			for(byte j=-1; j<=1; ++j){
				offset[0] = (int) (currentPoint[0] + j*currentSin);
				offset[1] = (int) currentPoint[1];
				offset[2] = (int) (currentPoint[2] + j*currentCos);
				if(BlockHelper.canPlaceBlockAt(worldObj, offset[0], offset[1], offset[2])){
					blockList.add(new int[] {offset[0], offset[1], offset[2], (int) (currentPoint[1]%1)*16});
				}else{
					if(!Arrays.equals(linkedCurve.blockEndPoint, offset)){
						return offset;
					}
				}
			}
		}
		//TODO spawn TES and blocks.
		return null;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox(){
		return INFINITE_EXTENT_AABB;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared(){
        return 65536.0D;
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.isPrimary = tagCompound.getBoolean("isPrimary");
        this.angle = tagCompound.getFloat("angle");
        int[] linkedFlagCoords = tagCompound.getIntArray("linkedFlagCoords");
        if(tagCompound.getIntArray("linkedFlagCoords").length != 0){
        	linkedCurve = new MFSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, linkedFlagCoords, tagCompound.getFloat("angle"), tagCompound.getFloat("linkedFlagAngle"));
        }else{
        	linkedCurve = null;
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("isPrimary", this.isPrimary);
        tagCompound.setFloat("angle", angle);
        if(linkedCurve != null){
        	tagCompound.setIntArray("linkedFlagCoords", linkedCurve.blockEndPoint);
        	tagCompound.setFloat("linkedFlagAngle", linkedCurve.endAngle);
        }
    }
}
