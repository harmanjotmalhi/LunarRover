/**
 * Earth-Moon system environment model.
 *
 * Provides an environment model for the two-body Earth-Moon system.  This
 * code uses the Java Astrodynamics Toolkit (JAT).
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.environment;

// Smackdown utility classes.
import see.smackdown.reference_frame.*;
import see.smackdown.utilities.*;

// JAT utility classes.
import jat.coreNOSA.math.MatrixVector.data.VectorN;

// JAT models.
import jat.coreNOSA.constants.*;
import jat.coreNOSA.spacetime.Time;
import jat.coreNOSA.ephemeris.DE405;
import jat.coreNOSA.ephemeris.DE405_Body;

public class EarthMoonSystem {
   
   public class Barycenter {
      ReferenceFrame inertial;
      ReferenceFrame rotating;
      public Barycenter (
         String parent_name,
         String inertial_name,
         String rotating_name )
      {
         inertial = new ReferenceFrame( parent_name, inertial_name );
         rotating = new ReferenceFrame( inertial_name, rotating_name );
      }
   }

   // Define the Earth, Moon and Earth-Moon Barycenter frame.
   public Earth      earth;
   public Moon       moon;
   public Barycenter barycenter;

   // Define the L2 Lagrange point reference frame.
   public ReferenceFrame l2_frame;

   // Lagrange point working variables.
   private double  body_mass_ratio;
   private double  system_mu_ratio;
   private double  L2_to_body2_dist_ratio;
   private double  body2_to_L2_scale_factor;

   // Default constructor.
   public EarthMoonSystem()
   {
      this( 0.0 );
   }


   // UTC time standard constructor.
   public EarthMoonSystem( double mjd_utc )
   {
      // Instantiate the Earth-Moon barycentric reference frames.
      this.barycenter = new Barycenter( "SolarSystemBarycentricInertial",
                                        "EarthMoonBarycentricInertial",
                                        "EarthMoonBarycentricRotating" );

      // Instantiate an Earth.
      this.earth = new Earth( mjd_utc );

      // Instantiate a Moon.
      this.moon = new Moon();

      // Instantiate an Earth-Moon L2 reference frame.
      this.l2_frame = new ReferenceFrame( "EarthMoonBarycentricInertial",
                                          "EarthMoonL2Rotating"          );

   }


   // JAT Time object constructor.
   public EarthMoonSystem(
      Time time )
   {
      // Instantiate the Earth-Moon barycentric reference frames.
      this.barycenter = new Barycenter( "SolarSystemBarycentricInertial",
                                        "EarthMoonBarycentricInertial",
                                        "EarthMoonBarycentricRotating" );

      // Instantiate an Earth.
      this.earth = new Earth( time );

      // Instantiate an Moon.
      this.moon = new Moon();

      // Instantiate an Earth-Moon L2 reference frame.
      this.l2_frame = new ReferenceFrame( "EarthMoonBarycentricInertial",
                                          "EarthMoonL2Rotating"          );

   }


   public void initialize(
      Time  ephem_time,
      DE405 ephemeris   )
   {

      // First we update the Earth-Moon barycentric inertial frame.
      this.update_barycenter_inertial( ephem_time, ephemeris );

      // Next we update the Earth reference frames.
      this.earth.update( this.barycenter.inertial, ephem_time, ephemeris );

      // Next, we update the Moon reference frames.
      this.moon.update( this.barycenter.inertial, ephem_time, ephemeris );

      // First we update the Earth-Moon barycentric rotating frame.
      this.update_barycenter_rotating( ephem_time );

      // Next, we initialize the L2 frame.
      this.initialize_L2_frame();

      // Finally, we update the L2 reference frame.
      this.update_L2_frame( ephem_time );

      return;
   }


   private void initialize_L2_frame()
   {

      // Set up body mass ratio and mu ratios of the system; these are constant
      // parameters that will enable calculation of position and velocity of
      // the managed frame with respect to the system barycenter (note that the
      // system barycenter is the direct parent to this frame)
      body_mass_ratio = IERS_1996.Moon_Earth_Ratio;
      system_mu_ratio = 1.0 + body_mass_ratio;

      // Set up the ratio of (L2 dist from body 1) / (body 2 dist from body 1);
      // requires solution of the fifth-order equation
      //   (1+a)*x^5 + (3+2a)*x^4 + (3+a)*x^3 -a*x^2 - 2a*x - a = 0
      // where alpha is the body mass ratio (as defined above) and x is the
      // desired distance ratio. Solve for the real solution for x via
      // Newton-Raphson. 
      double alpha = body_mass_ratio;
      double x5_coeff = 1.0 + alpha;
      double x4_coeff = 3.0 + 2.0*alpha;
      double x3_coeff = 3.0 + alpha;
      double x2_coeff = alpha;
      double x1_coeff = 2.0*alpha;
      double x0_coeff = alpha;

      double x_real = 0.2;
      double d_x = 1.0;
      double func;
      double d_func;

      while ((d_x > 1e-15) || (d_x < -1e-15)) {
         func  = x5_coeff*Math.pow(x_real,5.0) + x4_coeff*Math.pow(x_real,4.0)
               + x3_coeff*Math.pow(x_real,3.0) - x2_coeff*Math.pow(x_real,2.0)
               - x1_coeff*x_real          - x0_coeff;

         d_func = 5.0*x5_coeff*Math.pow(x_real,4.0) + 4.0*x4_coeff*Math.pow(x_real,3.0)
                + 3.0*x3_coeff*Math.pow(x_real,2.0) - 2.0*x2_coeff*x_real
                -x1_coeff;

         d_x = -func / d_func;
         x_real += d_x;
      }
      this.L2_to_body2_dist_ratio = x_real;

      // Build final overall constant to be used to scale position and velocity
      // of body 2 (wrt system barycenter) to the pos/vel of the L2 point (also
      // with respect to the system barycenter).
      body2_to_L2_scale_factor = 1.0 + (x_real * system_mu_ratio);


      // Extract the barycenter-to-body2 position and velocity vectors; NOTE
      // this assumes that the barycenter inertial frame is the parent frame
      // to body 2, which should be valid for all 3-body systems
      // Set initial pos/vel of the center of the L2 frame wrt the barycenter
      Vector3.scale( moon.inertial.position,
                     body2_to_L2_scale_factor,
                     l2_frame.position      );
      Vector3.scale( moon.inertial.velocity,
                     body2_to_L2_scale_factor,
                     l2_frame.velocity      );

      return;

   }


   public void update(
      Time  ephem_time,
      DE405 ephemeris   )
   {

      // First we update the Earth-Moon barycentric inertial frame.
      this.update_barycenter_inertial( ephem_time, ephemeris );

      // Next we update the Earth reference frames.
      this.earth.update( this.barycenter.inertial, ephem_time, ephemeris );

      // Next, we update the Moon reference frames.
      this.moon.update( this.barycenter.inertial, ephem_time, ephemeris );

      // First we update the Earth-Moon barycentric rotating frame.
      this.update_barycenter_rotating( ephem_time );

      // Finally, we update the L2 reference frame.
      this.update_L2_frame( ephem_time );

      return;

   }


   private void update_barycenter_inertial(
      Time  ephem_time,
      DE405 ephemeris   )
   {
      VectorN em_pos_vel; // Position and velocity vector of EM baricenter.

      // Get the JAT Earth-Moon barycenter position and velocity.
      // NOTE: This will be in kilometers in the Solar System Barycenter frame.
      em_pos_vel = ephemeris.get_planet_posvel( DE405_Body.EM_BARY,
                                                ephem_time         );

      //
      // Update the barycenter inertial frame.
      //
      // Get position and velocity meters from ephemeris.
      this.barycenter.inertial.position[0] = em_pos_vel.x[0] * Planet.KM2M;
      this.barycenter.inertial.position[1] = em_pos_vel.x[1] * Planet.KM2M;
      this.barycenter.inertial.position[2] = em_pos_vel.x[2] * Planet.KM2M;
      this.barycenter.inertial.velocity[0] = em_pos_vel.x[3] * Planet.KM2M;
      this.barycenter.inertial.velocity[1] = em_pos_vel.x[4] * Planet.KM2M;
      this.barycenter.inertial.velocity[2] = em_pos_vel.x[5] * Planet.KM2M;

      // By definition, inertial frames are aligned.
      this.barycenter.inertial.attitude.make_identity();
      Matrix3x3.identity( this.barycenter.inertial.T_parent_body );
      this.barycenter.inertial.attitude_rate[0] = 0.0;
      this.barycenter.inertial.attitude_rate[1] = 0.0;
      this.barycenter.inertial.attitude_rate[2] = 0.0;

      // Set the time.
      this.barycenter.inertial.time = ephem_time.mjd_tt() - Planet.MJD_TJD_OFFSET;

      return;

   }


   private void update_barycenter_rotating(
      Time  ephem_time )
   {

      //
      // Update the barycenter rotating frame.
      //
      // Bt definition, the roating frame is centered on the inertial frame.
      this.barycenter.rotating.position[0] = 0.0;
      this.barycenter.rotating.position[1] = 0.0;
      this.barycenter.rotating.position[2] = 0.0;
      this.barycenter.rotating.velocity[0] = 0.0;
      this.barycenter.rotating.velocity[1] = 0.0;
      this.barycenter.rotating.velocity[2] = 0.0;

      // Compute the attiude information for the rotating frame.
      compute_rotating_frame( moon.inertial, barycenter.rotating );

      // Set the time.
      this.barycenter.rotating.time = ephem_time.mjd_tt() - Planet.MJD_TJD_OFFSET;

      return;

   }


   private void  update_L2_frame(
      Time ephem_time )
   {

      //
      // Compute position and velocity of the L2 frame wrt the barycenter.
      // NOTE: This assumes that the barycenter inertial frame is the parent
      // frame, which should be valid for all 3-body systems.
      Vector3.scale( moon.inertial.position,
                     body2_to_L2_scale_factor,
                     l2_frame.position     );
      Vector3.scale( moon.inertial.velocity,
                     body2_to_L2_scale_factor,
                     l2_frame.velocity     );

      //
      // By definition, the L2 frame attitude and attitude rate are the same
      // as the Earth-Moon barycenter frame.
      //
      l2_frame.attitude.copy( barycenter.rotating.attitude );
      Matrix3x3.copy( barycenter.rotating.T_parent_body,
                      l2_frame.T_parent_body );
      Vector3.copy( barycenter.rotating.attitude_rate, l2_frame.attitude_rate );

      // Set the time.
      l2_frame.time = ephem_time.mjd_tt() - Planet.MJD_TJD_OFFSET;

      return;

   }


   private void  compute_rotating_frame(
      ReferenceFrame body_frame,
      ReferenceFrame rotating_frame )
   {
      // Declare the working variables.
      double    posn_mag_sq;
      double [] posn_unit_vector     = new double[3];
      double [] vel_unit_vector      = new double[3];
      double [] momentum_vector      = new double[3];
      double [] momentum_unit_vector = new double[3];
      double [] y_unit_axis          = new double[3];
      double [] ang_rate_in_inertial = new double[3];

      // Compute the position and velocity unit vectors.
      Vector3.copy( body_frame.position, posn_unit_vector );
      Vector3.normalize( posn_unit_vector );
      Vector3.copy( body_frame.velocity, vel_unit_vector );
      Vector3.normalize( vel_unit_vector );

      // Compute the angular momentum vector and momentum unit vector.
      Vector3.cross( body_frame.position,
                     body_frame.velocity,
                     momentum_vector );
      Vector3.copy( momentum_vector, momentum_unit_vector );
      Vector3.normalize( momentum_unit_vector );

      // Compute the Y-axis direction.
      Vector3.cross( momentum_unit_vector, posn_unit_vector, y_unit_axis );
      Vector3.normalize( y_unit_axis );

      // Build the transformation matrix.
      for (int ii = 0; ii < 3; ii++) {
         rotating_frame.T_parent_body[0][ii] = posn_unit_vector[ii];
         rotating_frame.T_parent_body[1][ii] = y_unit_axis[ii];
         rotating_frame.T_parent_body[2][ii] = momentum_unit_vector[ii];
      }

      // Compute the corresponding quaternion.
      rotating_frame.attitude.left_quat_from_transform( rotating_frame.T_parent_body );

      // Compute the EM L2 rotation vector.
      posn_mag_sq = Vector3.vmagsq( body_frame.position );
      Vector3.scale( momentum_vector, 1.0/posn_mag_sq, ang_rate_in_inertial );
      Vector3.transform( rotating_frame.T_parent_body,
                         ang_rate_in_inertial,
                         rotating_frame.attitude_rate );

      return;

   }


}
