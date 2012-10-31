package org.geotools.process.external;

import org.geotools.process.external.saga.SagaProcess;

public class SagaProcessGroup extends AppSpecificProcessGroup {

	@Override
	public void addProcess(ExternalProcess proc) {
		if (proc instanceof SagaProcess){
			super.addProcess(proc);
		}		
	}
	

}
