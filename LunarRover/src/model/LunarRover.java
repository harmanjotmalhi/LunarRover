package model;

import skf.model.object.annotations.Attribute;
import skf.model.object.annotations.ObjectClass;
import skf.coder.HLAunicodeStringCoder;

@ObjectClass(name = "PhysicalEntity.LunarRover")
public class LunarRover {
	
	@Attribute(name = "entity_name", coder = HLAunicodeStringCoder.class)
	private String entity_name = null;
	
	@Attribute(name = "parent_reference_frame", coder = HLAunicodeStringCoder.class)
	private String parent_reference_frame = null;
	
	@Attribute(name = "entity_type", coder = HLAunicodeStringCoder.class)
	private String entity_type = null;
	
	@Attribute(name = "position", coder = HLAPositionCoder.class)
	private Position position = null;
	
	public LunarRover() {
		
	}

	public LunarRover(String entity_name, String parent_reference_frame, String entity_type, Position position) {
		
		this.entity_name = entity_name;
		this.parent_reference_frame = parent_reference_frame;
		this.entity_type = entity_type;
		this.position = position;
	}

	public String getEntity_name() {
		return entity_name;
	}

	public void setEntity_name(String entity_name) {
		this.entity_name = entity_name;
	}

	public String getParent_reference_frame() {
		return parent_reference_frame;
	}

	public void setParent_reference_frame(String parent_reference_frame) {
		this.parent_reference_frame = parent_reference_frame;
	}

	public String getEntity_type() {
		return entity_type;
	}

	public void setEntity_type(String entity_type) {
		this.entity_type = entity_type;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return "LunarRover [entity_name=" + entity_name + ", parent_reference_frame=" + parent_reference_frame
				+ ", entity_type=" + entity_type + ", position=" + position + "]";
	}
	
	
	
}

