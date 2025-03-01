package core;

import java.net.MalformedURLException;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.AttributeNotOwned;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.ConnectionFailed;
import hla.rti1516e.exceptions.CouldNotCreateLogicalTimeFactory;
import hla.rti1516e.exceptions.CouldNotOpenFDD;
import hla.rti1516e.exceptions.ErrorReadingFDD;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.IllegalName;
import hla.rti1516e.exceptions.InconsistentFDD;
import hla.rti1516e.exceptions.InvalidLocalSettingsDesignator;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.ObjectClassNotPublished;
import hla.rti1516e.exceptions.ObjectInstanceNameInUse;
import hla.rti1516e.exceptions.ObjectInstanceNameNotReserved;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.exceptions.UnsupportedCallbackModel;
import model.LunarRover;
import siso.smackdown.FrameType;
import skf.config.Configuration;
import skf.core.SEEAbstractFederate;
import skf.core.SEEAbstractFederateAmbassador;
import skf.exception.PublishException;
import skf.exception.UpdateException;
import model.Position;

public class LunarRoverFederate extends SEEAbstractFederate implements Observer {
	
	private LunarRover lunarRover = null;
	private String local_settings_designator = null;
	

	public LunarRoverFederate(SEEAbstractFederateAmbassador seefedamb, LunarRover lunarRover) {
		super(seefedamb);
		this.lunarRover = lunarRover;
	}
	
	public void configureAndStart(Configuration config) throws ConnectionFailed, InvalidLocalSettingsDesignator, UnsupportedCallbackModel, CallNotAllowedFromWithinCallback, RTIinternalError, CouldNotCreateLogicalTimeFactory, FederationExecutionDoesNotExist, InconsistentFDD, ErrorReadingFDD, CouldNotOpenFDD, SaveInProgress, RestoreInProgress, NotConnected, MalformedURLException, FederateNotExecutionMember, NameNotFound, InvalidObjectClassHandle, AttributeNotDefined, ObjectClassNotDefined, InstantiationException, IllegalAccessException, IllegalName, ObjectInstanceNameInUse, ObjectInstanceNameNotReserved, ObjectClassNotPublished, AttributeNotOwned, ObjectInstanceNotKnown, PublishException, UpdateException{
			
		// 1. configure the skf
		super.configure(config);
			
		// 2. connect to the Pitch RTI
		local_settings_designator = "crcHost="+config.getCrcHost()+"\ncrcPort="+config.getCrcPort();
		super.connectOnRTI(local_settings_designator);
		
		// 3. Join the SEE Federation Execution
		super.joinIntoFederationExecution();
		
		// 4. subscribe the subject
		super.subscribeSubject(this);
		
		// 5. publish the LunarRover object
		super.publishElement(lunarRover);
		
		// 6. subscribe the "MoonCentricFixed" ReferenceFrame
		super.subscribeReferenceFrame(FrameType.MoonCentricFixed);
		
		// 7. Execution Loop
		super.startExecution();
		
		try {
			System.out.println("Press any key to disconnect the LunarRover Federate from the SEE Federation Execution");
			new Scanner(System.in).next();
			super.unsubscribeSubject(this);
			super.diconnectFromRTI();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	
	@Override
	public void update(Observable o, Object arg1) {

		System.out.println("The LunarRover Federate has received an update");
		System.out.println(arg1);
	}
	

	@Override
	protected void doAction() {
		Position curr_pos = this.lunarRover.getPosition();
		curr_pos.setX(curr_pos.getX()+10);
		
		try {
			super.updateElement(lunarRover);
		} catch (FederateNotExecutionMember | NotConnected | AttributeNotOwned | AttributeNotDefined
				| ObjectInstanceNotKnown | SaveInProgress | RestoreInProgress | RTIinternalError | IllegalName
				| ObjectInstanceNameInUse | ObjectInstanceNameNotReserved | ObjectClassNotPublished
				| ObjectClassNotDefined | UpdateException e) {
			e.printStackTrace();
		}
	}
	

}
