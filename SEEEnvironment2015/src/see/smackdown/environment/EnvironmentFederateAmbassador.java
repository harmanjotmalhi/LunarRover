/**
 * Environment Federate Ambassador.
 *
 * Provides the HLA Federate Ambassador interface for the SimSmackdown
 * Environment federate.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.environment;

// Java utility classes.
import java.util.HashMap;
import java.util.Map;

// Smackdown utility classes.
import see.smackdown.reference_frame.*;

// HLA 1516e classes.
import hla.rti1516e.time.*;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;


class EnvironmentFederateAmbassador extends NullFederateAmbassador {

   // This map holds references to the published reference frames.
   public final Map<String, ReferenceFrameObject> published_frames = new HashMap<String, ReferenceFrameObject>();

   // This map holds references to the discovered reference frames.
   private final Map<ObjectInstanceHandle, ReferenceFrameObject> discovered_frames = new HashMap<ObjectInstanceHandle, ReferenceFrameObject>();

   // This provides a mapping between instance names and know reference frames.
   private final Map<String, ReferenceFrame> expected_frames = new HashMap<String, ReferenceFrame>();

   // DEBUG flag.
   private static final boolean DEBUG = false;

   // Declare federate ambassador working variables.
   public EncoderFactory   encoder_factory;
   public HLAinteger64Time logical_time;
   public boolean          is_time_constrained    = false;
   public boolean          is_time_regulating     = false;
   public boolean          time_can_advance       = false;
   public boolean          wait_on_init_data      = true;
   public boolean          wait_on_init_discovery = true;
   public String           federate_name = "SimSmackDown Environment";


   // Environment default constructor
   public EnvironmentFederateAmbassador(
      String fed_name )
   {
      // Set the federate name.
      federate_name = new String( fed_name );
   }


   // Method to add an expected reference frames.  This is used to map
   // reference frames to discovered reference frame objects.
   public void add_expected_frame(
      ReferenceFrame ref_frame,
      String         name       )
   {

      // Only do something if reference frame is not NULL.
      if ( ref_frame != null ) {

         // Place the frame into the expected map.
         expected_frames.put( name, ref_frame );

      }

      return;

   }


   // Method to add new published reference frames.
   public ReferenceFrameObject add_published_frame(
      ReferenceFrame ref_frame,
      RTIambassador  rti_ambassador )
   {
      ReferenceFrameObject ref_frame_obj = null;

      // Only do something if reference frame is not NULL.
      if ( ref_frame != null ) {

         // Instantiate a new reference frame.
         ref_frame_obj = new ReferenceFrameObject( ref_frame,
                                                   ref_frame.ref_frame_name,
                                                   encoder_factory          );

         // Place the registered frame into the publish map.
         published_frames.put( ref_frame_obj.getInstanceName(), ref_frame_obj );

         // Register the frame instance with the federation execution.
         ref_frame_obj.register_frame( rti_ambassador );

      }

      return( ref_frame_obj );

   }


   @Override
   public void discoverObjectInstance(
      ObjectInstanceHandle instance_handle,
      ObjectClassHandle    object_class,
      String               instance_name )
   {
      // Declare the reference frame object to create.
      ReferenceFrameObject ref_frame_obj = null;

      // Declare the reference frame that it is associated with.
      ReferenceFrame expected_frame = null;
      
      // Place object on the appropriate list.
      if ( ReferenceFrameObject.matches( object_class ) ) {
         
         // Let the user know what we discovered.
         if ( DEBUG ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.print( "\" discovered a new ReferenceFrame object \"" );
            System.out.print( instance_name );
            System.out.println( "\"." );
         }

         // See if this reference frame is in the "expected" list.
         if ( expected_frames.containsKey( instance_name ) ) {

            // Get the expected reference frame.
            expected_frame = expected_frames.get( instance_name );

            // Instantiate an associated reference frame object.
            ref_frame_obj = new ReferenceFrameObject( expected_frame,
                                                      instance_name,
                                                      instance_handle,
                                                      encoder_factory );
         }
         else {
            // Unknown reference frame.
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.print( "\" discovered an unknown ReferenceFrame object \"" );
            System.out.print( instance_name );
            System.out.println( "\"." );
         }

         // Might want to check now if you're looking for a specific frame.
         if ( instance_name.compareTo( "EarthCentricInertial" ) == 0 ) {
            wait_on_init_discovery = false;
         }

         // Place the reference frame object in the map.
         if ( ref_frame_obj != null ){
            discovered_frames.put( instance_handle, ref_frame_obj );
         }
         
      }
      else {

         // Let the user know what we discovered.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.print( "\" discovered a new unknown object \"" );
         System.out.print( instance_name );
         System.out.println( "\"." );

      }
      
   }


   @Override
   public void objectInstanceNameReservationSucceeded(
      java.lang.String instance_name )
   throws FederateInternalError
   {
      ReferenceFrameObject published_frame;

      if ( published_frames.containsKey( instance_name ) ) {

         // Get the expected reference frame.
         published_frame = published_frames.get( instance_name );

         // Mark the object as having it's name successfully reserved.
         published_frame.name_reserved = true;

      }
      else {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.print( "\" Could not find published frame: \"" );
         System.out.print( instance_name );
         System.out.println( "\"." );
      }

      return;
   }


   @Override
   public void objectInstanceNameReservationFailed(
      java.lang.String instance_name )
   throws FederateInternalError
   {
      ReferenceFrameObject published_frame;

      if ( published_frames.containsKey( instance_name ) ) {

         // Get the expected reference frame.
         published_frame = published_frames.get( instance_name );

         // Mark the object as having it's name successfully reserved.
         published_frame.name_reservation_failed = true;

      }

      return;
   }


   @Override
   public void removeObjectInstance(
      ObjectInstanceHandle   instance_handle,
      byte[]                 user_tag,
      OrderType              sent_ordering,
      SupplementalRemoveInfo remove_info      )
   {

      ReferenceFrameObject ref_frame_obj;

      ref_frame_obj = discovered_frames.remove( instance_handle );

      if ( ref_frame_obj != null ) {
         // Let the user know what we're removing.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.print( "\" is removing the ReferenceFrame \"" );
         System.out.print( ref_frame_obj.getInstanceName() );
         System.out.println( "\"." );
      }
      else {
         // Let the user know something is wrong.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unknown object removed." );
      }

      return;

   }


   @Override
   public void reflectAttributeValues(
      ObjectInstanceHandle     instance_handle,
      AttributeHandleValueMap  attributes,
      byte[]                   user_tag,
      OrderType                sent_ordering,
      TransportationTypeHandle transport_handle,
      SupplementalReflectInfo  reflect_info    )
   {
      ReferenceFrameObject ref_frame_obj;
      String               ref_frame_name;
      
      // This callback is used for non-time-managed data.

      // Check to see if this is a ReferenceFrame
      if ( discovered_frames.containsKey( instance_handle ) ) {

         // See if the object is a ReferenceFrame.
         ref_frame_obj = discovered_frames.get( instance_handle );

         // Set the attributes for this ReferenceFrame.
         ref_frame_obj.setAttributes( attributes );

         // Get the reference frame name.
         ref_frame_name = ref_frame_obj.getInstanceName();
            
         // If this is the EarthCentricInertial object then we're initialized.
         if (    (ref_frame_name.compareTo("EarthCentricInertial")==0)
              && wait_on_init_data                                     ) {
            wait_on_init_data = false;
         }

      }
      else {

         // Let the user know something is wrong.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unknown object attribute reflection." );
         System.out.print( "   Object: "  );
         System.out.println( instance_handle.toString() );

      }

      return;

   }


   @Override
   public void reflectAttributeValues(
      ObjectInstanceHandle      instance_handle,
      AttributeHandleValueMap   attributes,
      byte[]                    user_tag,
      OrderType                 sent_ordering,
      TransportationTypeHandle  transport_handle,
      LogicalTime               time,
      OrderType                 received_ordering,
      SupplementalReflectInfo   reflect_info       )
   throws FederateInternalError
   {
      ReferenceFrameObject ref_frame_obj;
      String               ref_frame_name;
      
      // This callback is used for ReceiveOrder time-managed data.

      // Check to see if this is a ReferenceFrame
      if ( discovered_frames.containsKey( instance_handle ) ) {

         // See if the object is a ReferenceFrame.
         ref_frame_obj = discovered_frames.get( instance_handle );

         // Set the attributes for this ReferenceFrame.
         ref_frame_obj.setAttributes( attributes );

         // Get the reference frame name.
         ref_frame_name = ref_frame_obj.getInstanceName();
            
         // If this is the EarthCentricInertial object then we're initialized.
         if (    (ref_frame_name.compareTo("EarthCentricInertial")==0)
              && wait_on_init_data                                     ) {
            wait_on_init_data = false;
         }

      }
      else {

         // Let the user know something is wrong.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unknown object attribute reflection." );
         System.out.print( "   Object: "  );
         System.out.println( instance_handle.toString() );

      }

      return;

   }


   @Override
   public void reflectAttributeValues(
      ObjectInstanceHandle     instance_handle,
      AttributeHandleValueMap  attributes,
      byte[]                   user_tag,
      OrderType                sent_ordering,
      TransportationTypeHandle transport_handle,
      LogicalTime              time,
      OrderType                received_ordering,
      MessageRetractionHandle  retraction_handle,
      SupplementalReflectInfo  reflect_info       )
   throws FederateInternalError
   {
      ReferenceFrameObject ref_frame_obj;
      String               ref_frame_name;

      // This callback is used for TimeStampOrder time-managed data.

      // Check to see if this is a ReferenceFrame
      if ( discovered_frames.containsKey( instance_handle ) ) {

         // See if the object is a ReferenceFrame.
         ref_frame_obj = discovered_frames.get( instance_handle );

         // Set the attributes for this ReferenceFrame.
         ref_frame_obj.setAttributes( attributes );

         // Get the reference frame name.
         ref_frame_name = ref_frame_obj.getInstanceName();
            
         // If this is the EarthCentricInertial object then we're initialized.
         if (    (ref_frame_name.compareTo("EarthCentricInertial")==0)
              && wait_on_init_data                                     ) {
            wait_on_init_data = false;
         }

      }
      else {

         // Let the user know something is wrong.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unknown object attribute reflection." );
         System.out.print( "   Object: "  );
         System.out.println( instance_handle.toString() );

      }

      return;

   }


   @Override
   public void timeRegulationEnabled(
      LogicalTime time )
   throws FederateInternalError
   {
	  logical_time = (HLAinteger64Time)time;
      is_time_regulating = true;
   }


   @Override
   public void timeConstrainedEnabled(
      LogicalTime time )
   throws FederateInternalError
   {
	  logical_time = (HLAinteger64Time)time;
      is_time_constrained = true;
   }


   @Override
   public void timeAdvanceGrant(
      LogicalTime time )
   throws FederateInternalError
   {
      
      // Check granted time requested time.
      if ( ((HLAinteger64Time)time).compareTo( logical_time ) >= 0 ) {
         time_can_advance = true;
      }

      return;
   }


} // End of EnvironmentFederateAmbassador.

