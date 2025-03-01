/**
 * Solar system barycentric environment model.
 *
 * Provides the basic enviroment for the Sun and Solar system barycenter.
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

// JAT Earth model.
import jat.coreNOSA.spacetime.Time;
import jat.coreNOSA.ephemeris.DE405;
import jat.coreNOSA.ephemeris.DE405_Body;

public class SolarSystemBarycenter {

   // Declare the sun and the solar system barycenter reference frame data.
   public Planet         sun;
   public String         name;
   public ReferenceFrame inertial;

   // SolarSystemBarycenterary Ephemeris body designator,
   private DE405_Body id;

   public SolarSystemBarycenter()
   {
      name     = "SolarSystemBarycentricInertial";
      id       = DE405_Body.SOLAR_SYSTEM_BARY;
      inertial = new ReferenceFrame( "SolarSystemBarycentricInertial" );
      sun      = new Planet( this.inertial, "Sun", DE405_Body.SUN );
   }


   public void update(
      Time  ephem_time,
      DE405 ephemeris   )
   {
      VectorN ssb_pos_vel; // Position and velocity vector of Sun.
      ssb_pos_vel = ephemeris.get_planet_posvel( this.id, ephem_time  );

      // For now, this should be zero.  We may want to differentiate from
      // the sun inertial frame and the solar system barycenter frame in the
      // near future.
      inertial.position[0] = ssb_pos_vel.x[0] * Planet.KM2M;
      inertial.position[1] = ssb_pos_vel.x[1] * Planet.KM2M;
      inertial.position[2] = ssb_pos_vel.x[2] * Planet.KM2M;
      inertial.velocity[0] = ssb_pos_vel.x[3] * Planet.KM2M;
      inertial.velocity[1] = ssb_pos_vel.x[4] * Planet.KM2M;
      inertial.velocity[2] = ssb_pos_vel.x[5] * Planet.KM2M;

      // By definition, inertial frames are aligned.
      inertial.attitude.make_identity();
      Matrix3x3.identity( inertial.T_parent_body );
      inertial.attitude_rate[0] = 0.0;
      inertial.attitude_rate[1] = 0.0;
      inertial.attitude_rate[2] = 0.0;

      // Set the time.
      inertial.time = ephem_time.mjd_tt() - Planet.MJD_TJD_OFFSET;

      // Update the Sun.
      this.sun.update( ephem_time, ephemeris );

      return;

   }

}
