package com.vp.plugin.sample.chartdiagram.actions;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import com.vp.plugin.diagram.IChartDiagramUIModel;
import com.vp.plugin.diagram.IDiagramTypeConstants;
import com.vp.plugin.model.IChartCode;
import com.vp.plugin.model.IChartHeader;
import com.vp.plugin.model.IChartRelationship;
import com.vp.plugin.model.IChartType;
import com.vp.plugin.model.IChartTypeContainer;
import com.vp.plugin.model.IDFDataStore;
import com.vp.plugin.model.IDFProcess;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.ISimpleRelationship;
import com.vp.plugin.model.factory.IModelElementFactory;

public class ChartDiagramActionControl implements VPActionController {
	
	@Override
	public void performAction(VPAction arg0) {
		// Create chart diagram
		IChartDiagramUIModel chartDiagram = (IChartDiagramUIModel) ApplicationManager.instance().getDiagramManager().createDiagram(IDiagramTypeConstants.DIAGRAM_TYPE_CHART_DIAGRAM);
		
		// Create column header for the chart and specify its height 
		IChartHeader colHeader = IModelElementFactory.instance().createChartHeader();
		colHeader.setType(IChartHeader.TYPE_MODEL_ELEMENT);
		colHeader.setValue(IModelElementFactory.MODEL_TYPE_DF_PROCESS);
		chartDiagram.setColumnsAddresses(new String[] {colHeader.getId()});
		chartDiagram.setColumnHeaderHeight(120);
		
		// Create row header for the chart and specify its width
		IChartHeader rowHeader = IModelElementFactory.instance().createChartHeader();
		rowHeader.setType(IChartHeader.TYPE_MODEL_ELEMENT);
		rowHeader.setValue(IModelElementFactory.MODEL_TYPE_DF_DATA_STORE);
		chartDiagram.setRowsAddresses(new String[] {rowHeader.getId()});		
		chartDiagram.setRowHeaderWidth(80);
						
		// Obtain the current opening project
		IProject project = ApplicationManager.instance().getProjectManager().getProject();
		
		// Retrieve all DFD data stories and processes into array
		IModelElement[] processes = project.toAllLevelModelElementArray(IModelElementFactory.MODEL_TYPE_DF_PROCESS);
		IModelElement[] dataStores = project.toAllLevelModelElementArray(IModelElementFactory.MODEL_TYPE_DF_DATA_STORE);			
		
		// Walk through the model element array to retrieve model element's ID and store into a String array 
		String[] processIDs = toIDArray(processes);
		String[] dataStoreIDs = toIDArray(dataStores);
		
		// Add the process model IDs as column and data store IDs as row
		chartDiagram.setColumnIds(processIDs);
		chartDiagram.setRowIds(dataStoreIDs);
		
		// Retrieve chart type for CRUD chart from project default chart type container and specify it to chart diagram
		IChartTypeContainer chartTypeContainer = (IChartTypeContainer) project.toAllLevelModelElementArray(IModelElementFactory.MODEL_TYPE_CHART_TYPE_CONTAINER)[0];
		IChartType chartType = chartTypeContainer.getChartTypeByName("CRUD");
		chartDiagram.setChartTypeId(chartType.getId());
		
		// Retrieve chart codes for later use
		IChartCode codeCreate = null;
		IChartCode codeRead = null;
		IChartCode codeUpdate = null;
		IChartCode codeDelete = null;
		IChartCode[] codes = chartType.toCodeArray();
		
		for (IChartCode code : codes) {
			switch (code.getName()) {
			case "Create" :
				codeCreate = code;
				break;
			case "Read" :
				codeRead = code;
				break;
			case "Update" :
				codeUpdate = code;
				break;
			case "Delete" : 
				codeDelete = code;
				break;
			}			
		}		
				
		// Analyze the relationships on the process. 
		// If the relationship is to data store and named as "Create" then consider the process creating data.
		// If the relationship is to data store and named as "Delete" then consider the process deleting data.
		// If the relationship is to data store with no name then consider the process is updating data.
		// If the relationship from data store then consider the process is reading data.
		
		// Walk through the process models
		for (IModelElement modelElement : processes) {
			if (modelElement instanceof IDFProcess) {
				IDFProcess process = (IDFProcess) modelElement;
				// Analyze on the from relationship of the process
				ISimpleRelationship[] fromRelationships = process.toFromRelationshipArray();
				if (fromRelationships != null && fromRelationships.length > 0) {
					for (ISimpleRelationship fromRelationship : fromRelationships) {
						IModelElement toElement = fromRelationship.getTo();
						// If the relationship going to data store then create chart relationship 
						// and specify chart code according to the name of the relationship
						if (toElement instanceof IDFDataStore) {
							IChartRelationship chartRelationship = IModelElementFactory.instance().createChartRelationship();
							chartRelationship.setFrom(process);
							chartRelationship.setTo(toElement);
							// Consider process is creating data if the relationship named as "Create" 
							if ("Create".equals(fromRelationship.getName())) {
								chartRelationship.setCode(codeCreate);
							// Consider process is deleting data if the relationship named as "Delete" 
							} else if ("Delete".equals(fromRelationship.getName())) { 
								chartRelationship.setCode(codeDelete);
							// Otherwise consider process is updating data 
							} else {
								chartRelationship.setCode(codeUpdate);
							}
						}						
					}
				}
				// Analyze on the to relationship of the process
				ISimpleRelationship[] toRelationships = process.toToRelationshipArray();
				if (toRelationships != null && toRelationships.length > 0) {
					for (ISimpleRelationship toRelationship: toRelationships) {
						IModelElement fromElement = toRelationship.getFrom();
						// If the relationship is from data store then create 
						// chart relationship and specify chart code as update 
						if (fromElement instanceof IDFDataStore) {
							IChartRelationship chartRelationship = IModelElementFactory.instance().createChartRelationship();
							chartRelationship.setFrom(fromElement);
							chartRelationship.setTo(process);
							chartRelationship.setCode(codeRead);							
						}						
					}					
				}				
			}
		}		
		// Show up the chart diagram
		ApplicationManager.instance().getDiagramManager().openDiagram(chartDiagram);		
	}
	
	private String[] toIDArray(IModelElement[] elements) {
		if (elements != null) {
			String[] ids = new String[elements.length];
			for (int i = 0; i < elements.length; i++) {
				ids[i] = elements[i].getId();
			}
			return ids;
		}
		return null;
	}
	
	@Override
	public void update(VPAction arg0) {
		// TODO Auto-generated method stub
		
	}


}
