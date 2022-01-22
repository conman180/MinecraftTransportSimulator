package minecrafttransportsimulator.rendering.instances;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.systems.ConfigSystem;

public class RenderRoad extends ARenderEntityDefinable<TileEntityRoad>{
		
	@Override
	protected void renderModel(TileEntityRoad road, Matrix4dPlus transform, boolean blendingEnabled, float partialTicks){
		//Don't call super, we don't want to render the normal way.
		if(road.isActive() ^ blendingEnabled){
			//Render road components.
			for(RoadComponent component : road.components.keySet()){
				if(!road.componentRenderables.containsKey(component)){
					Point3dPlus position = new Point3dPlus();
					Point3dPlus rotation = new Point3dPlus();
					ItemRoadComponent componentItem = road.components.get(component);
					switch(component){
						case CORE_STATIC: {
							List<RenderableObject> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subName));
							int totalVertices = 0;
							for(RenderableObject object : parsedModel){
								totalVertices += object.vertices.capacity();
								for(int i=0; i<object.vertices.capacity(); i+=8){
									position.set(object.vertices.get(i+5), object.vertices.get(i+6), object.vertices.get(i+7));
									road.orientation.transform(position);
									object.vertices.put(i+5, (float) position.x);
									object.vertices.put(i+6, (float) position.y);
									object.vertices.put(i+7, (float) position.z);
								}
							}
							
							//Cache the model now that we know how big it is.
							FloatBuffer totalModel = FloatBuffer.allocate(totalVertices);
							for(RenderableObject object : parsedModel){
								totalModel.put(object.vertices);
							}
							totalModel.flip();
							road.componentRenderables.put(component, new RenderableObject(component.name(), componentItem.definition.getTextureLocation(componentItem.subName), new ColorRGB(), totalModel, true));
							break;
						}
						case CORE_DYNAMIC: {
							//Make sure our curve isn't null, we might have not yet created it.
							if(road.dynamicCurve != null){
								//Get model and convert to a single buffer of vertices.
								List<RenderableObject> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subName));
								int totalVertices = 0;
								for(RenderableObject object : parsedModel){
									totalVertices += object.vertices.capacity();
								}
								FloatBuffer parsedVertices = FloatBuffer.allocate(totalVertices);
								for(RenderableObject object : parsedModel){
									parsedVertices.put(object.vertices);
								}
								parsedVertices.flip();
								
								//Core components need to be transformed to wedges.
								Point3dPlus priorPosition = new Point3dPlus();
								Point3dPlus priorRotation = new Point3dPlus();
								Point3dPlus testPoint1 = new Point3dPlus();
								Point3dPlus testPoint2 = new Point3dPlus();
								Point3dPlus vertexOffsetPriorLine = new Point3dPlus();
								Point3dPlus vertexOffsetCurrentLine = new Point3dPlus();
								Point3dPlus segmentVector = new Point3dPlus();
								Point3dPlus renderedVertex = new Point3dPlus();
								float indexDelta = (float) (road.dynamicCurve.pathLength/Math.floor(road.dynamicCurve.pathLength/road.definition.road.segmentLength));
								boolean finalSegment = false;
								float priorIndex = 0;
								float currentIndex = 0;
								List<float[]> segmentVertices = new ArrayList<float[]>();
								while(!finalSegment){
									//If we are at the last index, do special logic to get the very end point.
									//We check here in case FPEs have accumulated and we won't end on the exact end segment.
									//Otherwise, increment normally.
									if(currentIndex != road.dynamicCurve.pathLength && currentIndex + indexDelta*1.25 > road.dynamicCurve.pathLength){
										currentIndex = road.dynamicCurve.pathLength;
										finalSegment = true;
									}else{
										currentIndex += indexDelta;
									}
									
									//Get current and prior curve position and rotation.
									//From this, we know how much to stretch the model to that point's rendering area.
									road.dynamicCurve.setPointToPositionAt(priorPosition, priorIndex);
									road.dynamicCurve.setPointToRotationAt(priorRotation, priorIndex);
									road.dynamicCurve.setPointToPositionAt(position, currentIndex);
									road.dynamicCurve.setPointToRotationAt(rotation, currentIndex);
									
									//If we are a really sharp curve, we might have inverted our model at the inner corner.
									//Check for this, and if we have done so, skip this segment.
									//If we detect this in the last 3 segments, skip right to the end.
									//This prevents a missing end segment due to collision.
									testPoint1.set(road.definition.road.borderOffset, 0, 0);
									testPoint1.rotateFine(priorRotation).add(priorPosition);
									testPoint2.set(road.definition.road.borderOffset, 0, 0);
									testPoint2.rotateFine(rotation).add(position);
									if(currentIndex != road.dynamicCurve.pathLength && ((position.x - priorPosition.x)*(testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z)*(testPoint2.z - testPoint1.z) < 0)){
										if(currentIndex + 3*indexDelta > road.dynamicCurve.pathLength){
											currentIndex = road.dynamicCurve.pathLength - indexDelta;
										}
										continue;
									}
									
									//Depending on the vertex position in the model, transform it to match with the offset rotation.
									//This depends on how far the vertex is from the origin of the model, and how big the delta is.
									//For all points, their magnitude depends on how far away they are on the Z-axis.
									for(int i=0; i<parsedVertices.capacity(); i+=8){
										float[] convertedVertexData = new float[8];
										
										//Add the normals and UVs first.  These won't change.
										parsedVertices.get(convertedVertexData, 0, 5);
										
										//Now convert the XYZ points.
										float x = parsedVertices.get();
										float y = parsedVertices.get();
										float z = parsedVertices.get();
										vertexOffsetPriorLine.set(x, y, 0);
										vertexOffsetPriorLine.rotateFine(priorRotation).add(priorPosition);
										vertexOffsetCurrentLine.set(x, y, 0);
										vertexOffsetCurrentLine.rotateFine(rotation).add(position);
										
										segmentVector.set(vertexOffsetCurrentLine);
										segmentVector.subtract(vertexOffsetPriorLine);
										segmentVector.multiply(Math.abs(z)/road.definition.road.segmentLength);
										
										renderedVertex.set(vertexOffsetPriorLine);
										renderedVertex.add(segmentVector);
										
										convertedVertexData[5] = (float) renderedVertex.x;
										convertedVertexData[6] = (float) renderedVertex.y;
										convertedVertexData[7] = (float) renderedVertex.z;
										
										//Add transformed vertices to the segment.
										segmentVertices.add(convertedVertexData);
									}
									//Rewind for next segment.
									parsedVertices.rewind();
									
									//Set the last index.
									priorIndex = currentIndex;
								}
								
								//Cache and compile the segments.
								FloatBuffer convertedVertices = FloatBuffer.allocate(segmentVertices.size()*8);
								for(float[] segmentVertex : segmentVertices){
									convertedVertices.put(segmentVertex);
								}
								convertedVertices.flip();
								road.componentRenderables.put(component, new RenderableObject(component.name(), componentItem.definition.getTextureLocation(componentItem.subName), new ColorRGB(), convertedVertices, true));
							}
							break;
						}
						default:
							break;
					}
				}
				RenderableObject object = road.componentRenderables.get(component);
				if(road.isActive()){
					object.color.setTo(ColorRGB.WHITE);
					object.alpha = 1.0F;
					object.isTranslucent = false;
				}else{
					object.color.setTo(ColorRGB.GREEN);
					object.alpha = 0.5F;
					object.isTranslucent = true;
				}
				object.transform.set(transform);
				object.render();
			}
			
			//If we are inactive render the blocking blocks and the main block.
			if(!road.isActive()){
				if(road.blockingBoundingBoxes.isEmpty()){
					road.blockingBoundingBoxes.add(new BoundingBox(new Point3dPlus(0, 0.75, 0), 0.15, 0.75, 0.15));
					for(Point3dPlus location : road.collidingBlockOffsets){
						road.blockingBoundingBoxes.add(new BoundingBox(location.copy().add(0, 0.55, 0), 0.55, 0.55, 0.55));
					}
				}
				boolean firstBox = true;
				for(BoundingBox blockingBox : road.blockingBoundingBoxes){
					if(firstBox){
						blockingBox.renderHolographic(transform, blockingBox.globalCenter, ColorRGB.BLUE);
						firstBox = false;
					}else{
						blockingBox.renderHolographic(transform, blockingBox.globalCenter, ColorRGB.RED);
					}
				}
			}else{
				//If we are in devMode and have hitboxes shown, render road bounds and colliding boxes.
				if(ConfigSystem.configObject.clientControls.devMode.value && InterfaceRender.shouldRenderBoundingBoxes()){
					if(road.devRenderables.isEmpty()){
						generateDevElements(road);
					}
					for(RenderableObject renderable : road.devRenderables){
						renderable.transform.set(transform);
						renderable.render();
					}
				}
			}
		}
	}
	
	@Override
	public void renderBoundingBoxes(TileEntityRoad road, Matrix4dPlus transform){
		super.renderBoundingBoxes(road, transform);
		//Render all collision boxes too.
		for(Point3dPlus blockOffset : road.collisionBlockOffsets){
			ABlockBase block = road.world.getBlock(road.position.copy().add(blockOffset));
			if(block instanceof BlockCollision){
				BoundingBox blockBounds = ((BlockCollision) block).blockBounds;
				Point3dPlus renderOffset = new Point3dPlus(blockOffset);
				//Need to offset by -0.5 as the collision box bounds is centered, but the offset isn't.
				renderOffset.add(-0.5, 0, -0.5);
				blockBounds.renderWireframe(road, transform, renderOffset, null);
			}
		}
	}
	
	private static void generateDevElements(TileEntityRoad road){
		//Create the information hashes.
		Point3dPlus point1 = new Point3dPlus();
		Point3dPlus point2 = new Point3dPlus();
		RenderableObject curveObject;
		if(road.dynamicCurve != null){
			//Render actual curve.
			curveObject = new RenderableObject(ColorRGB.GREEN, (int) (road.dynamicCurve.pathLength*10));
			for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
				road.dynamicCurve.setPointToPositionAt(point1, f);
				curveObject.addLine((float) point1.x, (float) point1.y, (float) point1.z, (float) point1.x, (float) point1.y + 1.0F, (float) point1.z);
			}
			curveObject.vertices.flip();
			road.devRenderables.add(curveObject);
			
			//Render the outer border bounds.
			curveObject = new RenderableObject(ColorRGB.CYAN, (int) (road.dynamicCurve.pathLength*10));
			for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
				road.dynamicCurve.setPointToRotationAt(point2, f);
				point1.set(road.definition.road.borderOffset, 0, 0);
				point1.rotateFine(point2);
				road.dynamicCurve.offsetPointByPositionAt(point1, f);
				curveObject.addLine((float) point1.x, (float) point1.y, (float) point1.z, (float) point1.x, (float) point1.y + 1.0F, (float) point1.z);
			}
			curveObject.vertices.flip();
			road.devRenderables.add(curveObject);
		}
		
		//Now render the lane curve segments.
		for(RoadLane lane : road.lanes){
			for(BezierCurve laneCurve : lane.curves){
				//Render the curve bearing indicator
				curveObject = new RenderableObject(ColorRGB.RED, 2);
				Point3dPlus bearingPos = laneCurve.endPos.copy().subtract(laneCurve.startPos).normalize().add(laneCurve.startPos);
				curveObject.addLine((float) laneCurve.startPos.x, (float) laneCurve.startPos.y, (float) laneCurve.startPos.z, (float) laneCurve.startPos.x, (float) laneCurve.startPos.y + 3.0F, (float) laneCurve.startPos.z);
				curveObject.addLine((float) laneCurve.startPos.x, (float) laneCurve.startPos.y + 3.0F, (float) laneCurve.startPos.z, (float) bearingPos.x, (float) bearingPos.y + 3.0F, (float) bearingPos.z);
				curveObject.vertices.flip();
				road.devRenderables.add(curveObject);
				
				//Render all the points on the curve.
				curveObject = new RenderableObject(ColorRGB.YELLOW, (int) (laneCurve.pathLength*10));
				for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
					laneCurve.setPointToPositionAt(point1, f);
					curveObject.addLine((float) point1.x, (float) point1.y, (float) point1.z, (float) point1.x, (float) point1.y + 1.0F, (float) point1.z);
				}
				curveObject.vertices.flip();
				road.devRenderables.add(curveObject);
			}
		}
		
		//Render the lane connections.
		for(RoadLane lane : road.lanes){
			for(List<RoadLaneConnection> curvePriorConnections : lane.priorConnections){
				BezierCurve currentCurve = lane.curves.get(lane.priorConnections.indexOf(curvePriorConnections));
				for(RoadLaneConnection priorConnection : curvePriorConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(priorConnection.tileLocation);
					if(otherRoad != null){
						RoadLane otherLane = otherRoad.lanes.get(priorConnection.laneNumber);
						if(otherLane != null){
							curveObject = new RenderableObject(ColorRGB.PINK, 3);
							
							//Get the first connection point.
							currentCurve.setPointToPositionAt(point1, 0.5F);
							
							//Get the other connection point.
							BezierCurve otherCurve = otherLane.curves.get(priorConnection.curveNumber);
							otherCurve.setPointToPositionAt(point2, priorConnection.connectedToStart ? 0.5F : otherCurve.pathLength - 0.5F);
							point2.add(otherLane.road.position).subtract(road.position);
							
							//Render U-shaped joint.
							curveObject.addLine((float) point1.x, (float) point1.y + 3.0F, (float) point1.z, (float) point1.x, (float) point1.y + 0.5F, (float) point1.z);
							curveObject.addLine((float) point1.x, (float) point1.y + 0.5F, (float) point1.z, (float) point2.x, (float) point2.y + 0.5F, (float) point2.z);
							curveObject.addLine((float) point2.x, (float) point2.y + 0.5F, (float) point2.z, (float) point2.x, (float) point2.y + 2.0F, (float) point2.z);
							curveObject.vertices.flip();
							road.devRenderables.add(curveObject);
						}
					}
				}
			}
		}
		for(RoadLane lane : road.lanes){
			for(List<RoadLaneConnection> curveNextConnections : lane.nextConnections){
				BezierCurve currentCurve = lane.curves.get(lane.nextConnections.indexOf(curveNextConnections));
				for(RoadLaneConnection nextConnection : curveNextConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(nextConnection.tileLocation);
					if(otherRoad != null){
						RoadLane otherLane = otherRoad.lanes.get(nextConnection.laneNumber);
						if(otherLane != null){
							curveObject = new RenderableObject(ColorRGB.ORANGE, 3);
							
							//Get the first connection point.
							currentCurve.setPointToPositionAt(point1, currentCurve.pathLength - 0.5F);
							
							//Get the other connection point.
							BezierCurve otherCurve = otherLane.curves.get(nextConnection.curveNumber);
							otherCurve.setPointToPositionAt(point2, nextConnection.connectedToStart ? 0.5F : otherCurve.pathLength - 0.5F);
							point2.add(otherLane.road.position).subtract(road.position);
							
							//Render U-shaped joint.
							curveObject.addLine((float) point1.x, (float) point1.y + 3.0F, (float) point1.z, (float) point1.x, (float) point1.y + 0.5F, (float) point1.z);
							curveObject.addLine((float) point1.x, (float) point1.y + 0.5F, (float) point1.z, (float) point2.x, (float) point2.y + 0.5F, (float) point2.z);
							curveObject.addLine((float) point2.x, (float) point2.y + 0.5F, (float) point2.z, (float) point2.x, (float) point2.y + 2.0F, (float) point2.z);
							curveObject.vertices.flip();
							road.devRenderables.add(curveObject);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void adjustPositionOrientation(TileEntityRoad road, Point3dPlus entityPositionDelta, Matrix4dPlus interpolatedOrientation, float partialTicks){
		//Undo orientation.  We don't want to rotate for this render call as all our curve data is absolute reference.
		//We also don't call super, as that would add a 1/2 offset to us and that masks the true position of this road's curve points.
		interpolatedOrientation.resetTransforms();
	}
}
