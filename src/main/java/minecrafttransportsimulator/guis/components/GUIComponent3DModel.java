package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.rendering.components.RenderableObject;

/**Custom #D model render class.  This allows for rendering a parsed model into a GUI.
 * Mainly used to render vehicles, though can be used for other models if desired.
 * This class keeps a list of all parsed models for caching, as this allows for faster
 * switching as we don't need to parse the models each time we view them.  These lists
 * are cleared when the GUI containing this component is un-loaded.  Note that the
 * model and texture associated with this component may change while this component
 * is still active.  This is to allows us to use one component to render changing
 * models, say in a crafting bench for instance.  The same reasoning applies
 * for why the position is not static (though it is required at construction).
 *
 * @author don_bruce
 */
public class GUIComponent3DModel extends AGUIComponent{
	/**Parsed vertex indexes.  Keyed by model name.*/
	private static final Map<String, RenderableObject> modelParsedObjects = new HashMap<String, RenderableObject>();
	private static final Map<String, Float> modelScalingFactors = new HashMap<String, Float>();
	private static final Matrix4d ISOMETRIC_TRANSFORM = generateIsometricTransform();
	
	public final float scaleFactor;
	public final boolean isometric;
	public final boolean staticScaling;
	
	public boolean spin;
	public float scale;
	public String modelLocation;
	public String textureLocation;
	    	
	public GUIComponent3DModel(int x, int y, float scaleFactor, boolean isometric, boolean spin, boolean staticScaling){
		super(x, y, 0, 0);
		this.scaleFactor = scaleFactor;
		this.isometric = isometric;
		this.spin = spin;
		this.staticScaling = staticScaling;
	}
	
	@Override
	public int getZOffset(){
		return MODEL_DEFAULT_ZOFFSET;
	}
	
	/**
	 *  Renders the model that this component defines.
	 */
    @Override
	public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks){
		if(!blendingEnabled && modelLocation != null){
			if(!modelParsedObjects.containsKey(modelLocation)){
				List<RenderableObject> parsedObjects = AModelParser.parseModel(modelLocation);
				//Remove any windows and "commented" objects from the model.  We don't want to render those.
				parsedObjects.removeIf(object -> object.name.toLowerCase().contains("window") || object.name.startsWith("#"));
				
				//Get the min/max vertex values for the model so we know how much to scale it.
				//Also get how many vertices are in the model total for the final buffer.
				float minX = 999;
				float maxX = -999;
				float minY = 999;
				float maxY = -999;
				float minZ = 999;
				float maxZ = -999;
				int totalVertices = 0;
				for(RenderableObject parsedObject : parsedObjects){
					totalVertices += parsedObject.vertices.capacity();
					for(int i=0; i<parsedObject.vertices.capacity(); i+=8){
						float xCoord = parsedObject.vertices.get(i+5);
						float yCoord = parsedObject.vertices.get(i+6);
						float zCoord = parsedObject.vertices.get(i+7);
						minX = Math.min(minX, xCoord);
						maxX = Math.max(maxX, xCoord);
						minY = Math.min(minY, yCoord);
						maxY = Math.max(maxY, yCoord);
						minZ = Math.min(minZ, zCoord);
						maxZ = Math.max(maxZ, zCoord);
					}
				}
				float globalMax = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
				modelScalingFactors.put(modelLocation, globalMax > 1.5 ? 1.5F/globalMax : 1.0F);
				
				//Cache the model now that we know how big it is.
				FloatBuffer totalModel = FloatBuffer.allocate(totalVertices);
				for(RenderableObject parsedObject : parsedObjects){
					totalModel.put(parsedObject.vertices);
				}
				totalModel.flip();
				RenderableObject combinedObject = new RenderableObject("model", textureLocation, ColorRGB.WHITE, totalModel, true);
				modelParsedObjects.put(modelLocation, combinedObject);
			}
			RenderableObject object = modelParsedObjects.get(modelLocation);
			object.transform.resetTransforms();
			object.transform.translate(position);
			if(isometric){
				object.transform.combine(ISOMETRIC_TRANSFORM);
			}
			if(spin){
				object.transform.rotate((36*System.currentTimeMillis()/1000)%360, 0, 1, 0);
			}
			if(!staticScaling){
				scale = modelScalingFactors.get(modelLocation);
			}
			object.transform.scale(scale*scaleFactor);
			object.texture = textureLocation;
			object.disableLighting = renderBright;
			object.render();
		}
    }
    
    /**
	 *  Clear the caches.  Call this when closing the GUI this component is a part of to free up RAM.
	 */
    public static void clearModelCaches(){
    	modelParsedObjects.values().forEach(modelParsedObject -> modelParsedObject.destroy());
    	modelParsedObjects.clear();
    }
    
    private static Matrix4d generateIsometricTransform(){
    	Matrix4d transform = new Matrix4d();
    	transform.setIdentity();
    	transform.set(new AxisAngle4d(0, 1, 0, Math.toRadians(-45)));
    	Matrix4d transform2 = new Matrix4d();
    	transform2.setIdentity();
    	transform2.set(new AxisAngle4d(1, 0, -1, Math.toRadians(35.264)));
    	transform.mul(transform2);
    	return transform;
    }
}
