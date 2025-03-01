/**
 * Reference Frame Object model.
 *
 * Provides the HLA interface for a reference frame.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.reference_frame;

import hla.rti1516e.*;
import hla.rti1516e.encoding.*;
import hla.rti1516e.exceptions.*;

public class ReferenceFrameObject {

   //
   // Class data.
   //

   // Implement ReferenceFrame relationship as "Has-A".
   private ReferenceFrame ref_frame;

   // HLA object and attribute handles.
   private static ObjectClassHandle  obj_class_handle;
   private static AttributeHandleSet attributeSet;

   private static AttributeHandle name_handle;
   private static AttributeHandle parent_frame_handle;
   private static AttributeHandle trans_state_handle;
   private static AttributeHandle rot_state_handle;
   private static AttributeHandle time_handle;

   // HLA data encoding/decoding helpers.
   private HLAunicodeString            name_encoder;
   private HLAfloat64LE                double_encoder;
   private HLAfixedArray<HLAfloat64LE> vec_encoder;
   private HLAfixedArray<HLAfloat64LE> vec_dot_encoder;
   private HLAfixedArray<HLAfloat64LE> q_vec_encoder;
   private HLAfixedRecord              quat_encoder;
   private HLAfixedRecord              trans_state_encoder;
   private HLAfixedRecord              rot_state_encoder;

   // HLA object instance data.
   private       ObjectInstanceHandle instance_handle;
   private final String               instance_name;
   public        boolean              name_reserved = false;
   public        boolean              name_reservation_failed = false;

   // Internal class control variables.
   private static boolean       initialized = false;
   private static final boolean debug = true;


   // This constructor will register an object instance.
   public ReferenceFrameObject(
      ReferenceFrame frame,
      String         name,
      EncoderFactory encoder_factory )
   {
      // Associate the specified reference frame with this HLA object.
      ref_frame = frame;

      // Assign the instance name.
      instance_name = name;

      // Create the encoders.
      create_encoders( encoder_factory );

   }


   // This constructor is used for discovering an object instance.
   public ReferenceFrameObject(
      ReferenceFrame       frame,
      String               name,
      ObjectInstanceHandle handle,
      EncoderFactory       encoder_factory )
   {
      // Associate the specified reference frame with this HLA object.
      ref_frame = frame;

      // Assign the instance name and handle.
      instance_name   = name;
      instance_handle = handle;

      // Create the encoders.
      create_encoders( encoder_factory );

   }


   // This method is used to create the ReferenceFrame encoder/decoders.
   public void create_encoders (
      EncoderFactory encoder_factory )
   {
 
      // Create the attribute encoders/decoders.
      name_encoder = encoder_factory.createHLAunicodeString();
      double_encoder = encoder_factory.createHLAfloat64LE();
      vec_encoder = encoder_factory.createHLAfixedArray(
            encoder_factory.createHLAfloat64LE(),
            encoder_factory.createHLAfloat64LE(),
            encoder_factory.createHLAfloat64LE() );
      vec_dot_encoder = encoder_factory.createHLAfixedArray(
            encoder_factory.createHLAfloat64LE(),
            encoder_factory.createHLAfloat64LE(),
            encoder_factory.createHLAfloat64LE() );
      q_vec_encoder = encoder_factory.createHLAfixedArray(
            encoder_factory.createHLAfloat64LE(),
            encoder_factory.createHLAfloat64LE(),
            encoder_factory.createHLAfloat64LE() );
      quat_encoder = encoder_factory.createHLAfixedRecord();
      quat_encoder.add( double_encoder );
      quat_encoder.add( q_vec_encoder );
      trans_state_encoder = encoder_factory.createHLAfixedRecord();
      trans_state_encoder.add( vec_encoder );
      trans_state_encoder.add( vec_dot_encoder );
      rot_state_encoder = encoder_factory.createHLAfixedRecord();
      rot_state_encoder.add( quat_encoder );
      rot_state_encoder.add( vec_dot_encoder );

      return;

   }


   // This method is used to register named reference frames.
   public void register_frame (
      RTIambassador rti_ambassador )
   {

      // Register an object instance.
      try {

         // Reserve the name.
         rti_ambassador.reserveObjectInstanceName( instance_name );

         // Need to wait here until name reservation callback is recieved.
         while ( !name_reserved && !name_reservation_failed ) {
            Thread.yield();
         }
         if ( name_reservation_failed ) {
            System.out.print( "Object Instance \"" );
            System.out.print( instance_name );
            System.out.println( "\": Name Resevation Failed" );
         }

         // If name is reserved then register the instance.
         if ( name_reserved ) {
            instance_handle =
               rti_ambassador.registerObjectInstance( obj_class_handle,
                                                      instance_name );
         }

      } catch ( ObjectInstanceNameInUse oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Name In Use error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( ObjectInstanceNameNotReserved oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Reserved error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( ObjectClassNotPublished oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Published error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( ObjectClassNotDefined oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Class Not Defined error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( FederateNotExecutionMember oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Execution Member error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( NotConnected oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Connected error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( RTIinternalError oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": RTI Internal error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( Exception e ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
      }

      return;

   }


   @Override
   public String toString()
   {
      return instance_name;
   }
   
   public String getInstanceName()
   {
      return instance_name;
   }
   
   public ObjectInstanceHandle getInstanceHandle()
   {
      return instance_handle;
   }
   
   public AttributeHandleSet getAttributeHandleSet()
   {
      return attributeSet;
   }
    
   public static boolean matches( ObjectClassHandle other_class )
   {
      if ( initialized ) {
         return( obj_class_handle.equals(other_class) );
      } else {
         return( false );
      }
   }
   
   public static void initialize( RTIambassador rti_ambassador )
   {

      if ( initialized ) { return; }

      try {

         // Get a handle to the ReferenceFrame class.
         obj_class_handle =
            rti_ambassador.getObjectClassHandle( "ReferenceFrame" );

         // Get handles to all the ReferenceFrame attributes.
         name_handle =
            rti_ambassador.getAttributeHandle( obj_class_handle, "name" );

         parent_frame_handle =
            rti_ambassador.getAttributeHandle( obj_class_handle,
                                               "parent_name" );
         trans_state_handle =
            rti_ambassador.getAttributeHandle( obj_class_handle, 
                                               "translational_state" );
         rot_state_handle =
            rti_ambassador.getAttributeHandle( obj_class_handle, 
                                               "rotational_state" );
         time_handle =
            rti_ambassador.getAttributeHandle( obj_class_handle, "time" );

         // Generate an attribute handle set.
         attributeSet = rti_ambassador.getAttributeHandleSetFactory().create();
         attributeSet.add( name_handle );
         attributeSet.add( parent_frame_handle );
         attributeSet.add( trans_state_handle );
         attributeSet.add( rot_state_handle );
         attributeSet.add( time_handle );

      } catch ( Exception e ) {
         System.out.println("Failed to initialize ReferenceFrameObject.");
         System.out.println( e.getMessage() );
      }

      // Mark this object class as initialized.
      initialized = true;

      return;

   }
   
   public static void subscribe( RTIambassador rti_ambassador )
   {

      try {
         rti_ambassador.subscribeObjectClassAttributes( obj_class_handle,
                                                        attributeSet      );
      } catch ( Exception e ) {
         
      }
      
      return;
      
   }
   
   public static void publish( RTIambassador rti_ambassador )
   {

      try {
         rti_ambassador.publishObjectClassAttributes( obj_class_handle,
                                                      attributeSet      );
      } catch ( Exception e ) {
         
      }
      
      return;
      
   }


   @SuppressWarnings("unchecked")
   public void setAttributes( AttributeHandleValueMap attribute_values )
   {
      
      if ( debug ) {
         System.out.println( );
         System.out.print( "ReferenceFrame \"" );
         System.out.print( instance_name );
         System.out.println( "\" Started processing attribute updates." );
      }

      // Check for the name attribute.
      if ( attribute_values.containsKey( name_handle ) ) {

         // Decode the incoming name.
         try {
            name_encoder.decode( attribute_values.get( name_handle ) );
            ref_frame.ref_frame_name = name_encoder.getValue();
         } catch (DecoderException e) {
            System.out.println("Failed to decode reference frame name.");
         }
         
         if (debug ) {
            // Print out the time.
            System.out.print( "ReferenceFrame \"" );
            System.out.print( instance_name );
            System.out.print( "\" received name update:" );
            System.out.println( ref_frame.ref_frame_name );
         }

      }

      // Check for the parent frame name attribute.
      if ( attribute_values.containsKey( parent_frame_handle ) ) {

         // Decode the incoming name.
         try {
            name_encoder.decode( attribute_values.get( parent_frame_handle ) );
            ref_frame.parent_frame_name = name_encoder.getValue();
         } catch (DecoderException e) {
            System.out.println("Failed to decode parent refernce frame name.");
         }
         
         if (debug ) {
            // Print out the time.
            System.out.print( "ReferenceFrame \"" );
            System.out.print( instance_name );
            System.out.print( "\" received parent frame name update:" );
            System.out.println( ref_frame.parent_frame_name );
         }

      }

      // Check for the translational state attribute.
      if ( attribute_values.containsKey( trans_state_handle ) ) {

         // Decode the incoming translational state.
         try {
            
            // Local references to encoders embedded in fixedArrays.
            HLAfixedArray<HLAfloat64LE> hla_pos_vector;
            HLAfixedArray<HLAfloat64LE> hla_vel_vector;
            
            // Decode the incoming attribute.
            trans_state_encoder.decode( attribute_values.get( trans_state_handle ) );
            
            // Get references to embedded encoders.
            hla_pos_vector = (HLAfixedArray<HLAfloat64LE>)(trans_state_encoder.get(0));
            hla_vel_vector = (HLAfixedArray<HLAfloat64LE>)(trans_state_encoder.get(1));

            // Decode the position vector.
            ref_frame.position[0] = hla_pos_vector.get( 0 ).getValue();
            ref_frame.position[1] = hla_pos_vector.get( 1 ).getValue();
            ref_frame.position[2] = hla_pos_vector.get( 2 ).getValue();

            // Decode the velocity vector.
            ref_frame.velocity[0] = hla_vel_vector.get( 0 ).getValue();
            ref_frame.velocity[1] = hla_vel_vector.get( 1 ).getValue();
            ref_frame.velocity[2] = hla_vel_vector.get( 2 ).getValue();
            
         } catch (DecoderException e) {
            System.out.println("Failed to decode parent refernce frame translational state.");
         }
         
         if (debug ) {
            System.out.print( "ReferenceFrame \"" );
            System.out.print( instance_name );
            System.out.println( "\":" );
            System.out.println( "   received position update {m}: " );
            System.out.print( ref_frame.position[0] );
            System.out.print( ", " );
            System.out.print( ref_frame.position[1] );
            System.out.print( ", " );
            System.out.println( ref_frame.position[2] );
            System.out.println( "   received velocity update {m/s}: " );
            System.out.print( ref_frame.velocity[0] );
            System.out.print( ", " );
            System.out.print( ref_frame.velocity[1] );
            System.out.print( ", " );
            System.out.println( ref_frame.velocity[2] );
         }

      }

      // Check for the rotational state attribute.
      if ( attribute_values.containsKey( rot_state_handle ) ) {

         // Decode the incoming rotational state.
         try {
            // Local references to encoders embedded in fixedRecords.
            HLAfixedRecord              hla_quat;
            HLAfloat64LE                hla_q_scalar;
            HLAfixedArray<HLAfloat64LE> hla_q_vector;
            HLAfixedArray<HLAfloat64LE> hla_omega_vector;

            // Decode the incoming attribute.
            rot_state_encoder.decode( attribute_values.get(rot_state_handle) );

            // Get references to embedded encoders.
            hla_quat = (HLAfixedRecord)(rot_state_encoder.get(0));
            hla_q_scalar = (HLAfloat64LE)(hla_quat.get(0));
            hla_q_vector = (HLAfixedArray<HLAfloat64LE>)(hla_quat.get(1));
            hla_omega_vector = (HLAfixedArray<HLAfloat64LE>)(rot_state_encoder.get(1));

            // Decode the quaternion
            ref_frame.attitude.scalar    = hla_q_scalar.getValue();
            ref_frame.attitude.vector[0] = hla_q_vector.get( 0 ).getValue();
            ref_frame.attitude.vector[1] = hla_q_vector.get( 1 ).getValue();
            ref_frame.attitude.vector[2] = hla_q_vector.get( 2 ).getValue();

            // Compute the traditional attitude matrix.
            ref_frame.attitude.left_quat_to_transform(ref_frame.T_parent_body);

            // Decode the angular velocity vector.
            ref_frame.attitude_rate[0] = hla_omega_vector.get( 0 ).getValue();
            ref_frame.attitude_rate[1] = hla_omega_vector.get( 1 ).getValue();
            ref_frame.attitude_rate[2] = hla_omega_vector.get( 2 ).getValue();

         } catch (DecoderException e) {
            System.out.println("Failed to decode parent refernce frame translational state.");
         }
         
         if (debug ) {
            // Print out the time.
            System.out.print( "ReferenceFrame \"" );
            System.out.print( instance_name );
            System.out.println( "\":" );
            System.out.println( "   received attitude update {quaternion}: " );
            System.out.print( ref_frame.attitude.scalar );
            System.out.print( " | " );
            System.out.print( ref_frame.attitude.vector[0] );
            System.out.print( ", " );
            System.out.print( ref_frame.attitude.vector[1] );
            System.out.print( ", " );
            System.out.println( ref_frame.attitude.vector[2] );
            System.out.println( "   received rotational velocity update {r/s}: " );
            System.out.print( ref_frame.attitude_rate[0] );
            System.out.print( ", " );
            System.out.print( ref_frame.attitude_rate[1] );
            System.out.print( ", " );
            System.out.println( ref_frame.attitude_rate[2] );
         }

      }

      // Check for time attribute.
      if ( attribute_values.containsKey( time_handle )){

         // Decode the incoming time.
         try {
            double_encoder.decode( attribute_values.get( time_handle ) );
            ref_frame.time = double_encoder.getValue();
         } catch (DecoderException e) {
            System.out.println("Failed to decode refernce frame name.");
         }
         
         if (debug ) {
            // Print out the time.
            System.out.print( "ReferenceFrame \"" );
            System.out.print( instance_name );
            System.out.print( "\" received time update {s}:" );
            System.out.println( ref_frame.time );
         }
         
      }
      
      if ( debug ) {
         System.out.print( "RefernceFrame \"" );
         System.out.print( instance_name );
         System.out.println( "\" Finished processing attribute updates." );
         System.out.println( );
      }
   
      return;
      
   }


   @SuppressWarnings("unchecked")
   public AttributeHandleValueMap updateAttributes(
      RTIambassador rti_ambassador,
      LogicalTime   logical_time )
   {
      AttributeHandleValueMap attribute_values = null;

      try {

         // Get an AttributeHandleValueMap from the RTI ambassador.
         attribute_values =
            rti_ambassador.getAttributeHandleValueMapFactory().create(5); 

         // Encode the reference frame name.
         name_encoder.setValue( ref_frame.ref_frame_name );
         attribute_values.put( name_handle, name_encoder.toByteArray() );

         // Encode the parent reference frame name.
         if ( ref_frame.parent_frame_name != null ) {
            name_encoder.setValue( ref_frame.parent_frame_name );
            attribute_values.put( parent_frame_handle,
                                  name_encoder.toByteArray() );
         }

         //
         // Encode the translational state data.
         //
         // Get references to embedded encoders.
         HLAfixedArray<HLAfloat64LE> hla_pos_vector =
            (HLAfixedArray<HLAfloat64LE>)(trans_state_encoder.get(0));
         HLAfixedArray<HLAfloat64LE> hla_vel_vector =
            (HLAfixedArray<HLAfloat64LE>)(trans_state_encoder.get(1));

         // Encode the position vector.
         hla_pos_vector.get( 0 ).setValue( ref_frame.position[0] );
         hla_pos_vector.get( 1 ).setValue( ref_frame.position[1] );
         hla_pos_vector.get( 2 ).setValue( ref_frame.position[2] );

         // Encode the velocity vector.
         hla_vel_vector.get( 0 ).setValue( ref_frame.velocity[0] );
         hla_vel_vector.get( 1 ).setValue( ref_frame.velocity[1] );
         hla_vel_vector.get( 2 ).setValue( ref_frame.velocity[2] );

         // Put the encoded translational state into attribute value map.
         attribute_values.put( trans_state_handle,
                               trans_state_encoder.toByteArray() );

         //
         // Encode the rotational state data.
         //
         // Get references to embedded encoders.
         HLAfixedRecord              hla_quat =
            (HLAfixedRecord)(rot_state_encoder.get(0));
         HLAfloat64LE                hla_q_scalar =
            (HLAfloat64LE)(hla_quat.get(0));
         HLAfixedArray<HLAfloat64LE> hla_q_vector =
            (HLAfixedArray<HLAfloat64LE>)(hla_quat.get(1));
         HLAfixedArray<HLAfloat64LE> hla_omega_vector =
            (HLAfixedArray<HLAfloat64LE>)(rot_state_encoder.get(1));

         // Encode the quaternion
         hla_q_scalar.setValue( ref_frame.attitude.scalar );
         hla_q_vector.get( 0 ).setValue( ref_frame.attitude.vector[0] );
         hla_q_vector.get( 1 ).setValue( ref_frame.attitude.vector[1] );
         hla_q_vector.get( 2 ).setValue( ref_frame.attitude.vector[2] );

         // Encode the angular velocity vector.
         hla_omega_vector.get( 0 ).setValue( ref_frame.attitude_rate[0] );
         hla_omega_vector.get( 1 ).setValue( ref_frame.attitude_rate[1] );
         hla_omega_vector.get( 2 ).setValue( ref_frame.attitude_rate[2] );

         // Put the encoded rotational state into attribute value map.
         attribute_values.put( rot_state_handle,
                               rot_state_encoder.toByteArray() );

         // Encode the time stamp.
         double_encoder.setValue( ref_frame.time );
         attribute_values.put( time_handle, double_encoder.toByteArray() );

      } catch ( FederateNotExecutionMember oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Execution Member error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( NotConnected oi_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Connected error:" );
         System.out.println( oi_error.getMessage() );
      } catch ( Exception e ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
      }

      // Update the attribute values for this reference frame object.
      try {
         rti_ambassador.updateAttributeValues( this.instance_handle,
                                               attribute_values,
                                               null, logical_time );
      } catch ( InvalidLogicalTime update_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Invalid Logical Time error:" );
         System.out.println( update_error.getMessage() );
      } catch ( FederateNotExecutionMember update_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Execution Member error:" );
         System.out.println( update_error.getMessage() );
      } catch ( NotConnected update_error ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Not Connected error:" );
         System.out.println( update_error.getMessage() );
      } catch ( Exception e ) {
         System.out.print( "Object Instance \"" );
         System.out.print( instance_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
      }

      return( attribute_values );

   }

}
