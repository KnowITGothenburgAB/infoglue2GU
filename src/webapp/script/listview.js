// listview.js

//-------------------------------------
// These methods are used for the lists
// in the system where you can select 
// one or more.
//-------------------------------------

var lastRow = null;
var lastRowOriginalBgColor = null;
var selectedRow = null;

var selectedBgColor = "#DEDEDE"; // "#C2D0E2";

var checkedBgColor = "#777777";
var checkedBgColorHi = "#1478EB";

if (navigator.appName == "Netscape") {
  document.captureEvents(Event.CLICK);
}

function showWorking()
{
	//alert("parent: " + parent.frames["main"].document.title);
	var p = parent.frames["main"].document.getElementById("working");
	p.style.visibility = "visible";
	// alert("saving");
}

function listRowSelect(box,rowEl)
{
	var chkbox=document.getElementById(box);	
	chkbox.checked=true;

}

function listRowOn(rowEl)
{
	return ;
	if (lastRow != null)
	{
		lastRow.style.backgroundColor = lastRowOriginalBgColor;
	}

	lastRowOriginalBgColor = rowEl.style.backgroundColor;
	if (rowEl.style.backgroundColor == checkedBgColor)
		rowEl.style.backgroundColor = checkedBgColorHi;	
	else
		rowEl.style.backgroundColor = selectedBgColor;
		
	lastRow = rowEl;
}

function listRowHiLight(rowEl)
{
	// Anv�nds inte �n!
	return;
	if (rowEl.className.slice(0,6) == "marked") return

	if (rowEl.className.slice(0,2) != "hi")
		rowEl.className = "hi"+rowEl.className;
		
}


function listRowUnHiLight(rowEl)
{
	// Anv�nds inte �n!
	return;
	if (rowEl.className.slice(0,6) == "marked") return
	
	var rowClass = rowEl.className;
	if(rowClass.length > 2)
	{
		if (rowClass.slice(0,2) == "hi")
		{
			rowEl.className = rowClass.slice(2, rowClass.length);
		}
	}
}

function listRowMarked(rowEl)
{
	listRowUnHiLight(rowEl);
	
	if (rowEl.className.slice(0,6) != "marked")
		rowEl.className = "marked"+rowEl.className;
		
	// lastRowOriginalBgColor = rowEl.style.backgroundColor;
	lastRow = rowEl;
}


function listRowUnMarked(rowEl)
{
	var rowClass = rowEl.className;
	if(rowClass.length > 6)
	{
		if (rowClass.slice(0,6) == "marked")
		{
			rowEl.className = rowClass.slice(6, rowClass.length);
		}
	}
			
	// lastRowOriginalBgColor = rowEl.style.backgroundColor;
	lastRow = rowEl;
}

function listRowClear()
{
	if (lastRow != null)
	{
		if (lastRow != selectedRow)
		{
			lastRow.style.backgroundColor = lastRowOriginalBgColor;
		}
	}
}

function checkAll()
{
 	var field = document.objList.sel;
	for (i = 0; i < field.length; i++)
		field[i].checked = true ;
}

function uncheckAll()
{
	var field = document.listForm.sel;
	for (i = 0; i < field.length; i++)
		field[i].checked = false ;
}

function CheckUncheckSingle(row,chkbox)
{
	var rowEl=document.getElementById(row);
	if (chkbox.checked)
	{
		uncheckAll();
		chkbox.checked = true;
		if (lastRow != null)
		{
			listRowUnMarked(lastRow);
		}

		listRowMarked(rowEl);	
	}
	else
	{
		listRowUnMarked(rowEl);		
	}
}

function CheckUncheck(row,chkbox)
{
	var rowEl=document.getElementById(row);
	if (chkbox.checked)
	{
		listRowMarked(rowEl);
	}
	else
	{
		listRowUnMarked(rowEl);		
	}
}

function checkAllBoxes(element)
{
	if(element)
	{
		var length = element.length;
	  	if(length == null)
	  	{
	  		element.checked = true;
	  		rowId = element.getAttribute("rowId");
			listRowMarked(document.getElementById(rowId));
	  	}
	  	else
	  	{	
		 	var field = element;
		 	for (i = 0; i < field.length; i++)
			{
				field[i].checked = true;
				rowId = field[i].getAttribute("rowId");
				listRowMarked(document.getElementById(rowId));
			}
		}
	}
}

function uncheckAllBoxes(element)
{
	if(element)
	{
		var length = element.length;
	  	if(length == null)
	  	{
	  		element.checked = false;
	  		rowId = element.getAttribute("rowId");
			listRowUnMarked(document.getElementById(rowId));
	  	}
	  	else
	  	{	
		 	var field = element;
		 	for (i = 0; i < field.length; i++)
			{
				field[i].checked = false;
				rowId = field[i].getAttribute("rowId");
				listRowUnMarked(document.getElementById(rowId));
			}
		}
	}
}

//-----------------------------------------------
// This function reloads the toolbar above the current 
// active action so that buttons related to the view 
// can be shown and a suitable headline be written.
//-----------------------------------------------

function refreshManagementToolBar(toolbarKey, arguments)
{
	//alert('toolbarKey:' + toolbarKey);
	//toolbarKey 	= escape(toolbarKey);	
	toolbarKey 	= hexcode(toolbarKey);	
	parent.frames["toolbar"].location.href = 'ViewManagementToolToolBar.action?title=' + toolbarKey + '&toolbarKey=' + toolbarKey + '&' + arguments;
}


//-----------------------------------------------
// This function reloads the toolbar above the current 
// active action so that buttons related to the view 
// can be shown and a suitable headline be written.
//-----------------------------------------------

function refreshContentToolBar(title, toolbarKey, arguments, unrefreshedContentId, changeTypeId, newContentId)
{
	//alert('toolbarKey:' + toolbarKey);
	//title		  = escape(title);
	title		= hexcode(title);
	//toolbarKey 	= escape(toolbarKey);	
	toolbarKey 	= hexcode(toolbarKey);	
	parent.frames["toolbar"].location.href = 'ViewContentToolToolBar.action?title=' + title + '&toolbarKey=' + toolbarKey + '&' + arguments;
	if(unrefreshedContentId > 0)
	{
		//alert("About to call refresh on menu:" + unrefreshedContentId + ":" + changeTypeId + ":" + newContentId);
		parent.frames["menu"].refreshContent(unrefreshedContentId, changeTypeId, newContentId);
	}
}

var cmsContext = "";
function setCMSContext(context)
{
	cmsContext = context;
}

//-----------------------------------------------
// This function reloads the toolbar above the current 
// active action so that buttons related to the view 
// can be shown and a suitable headline be written.
//-----------------------------------------------

function refreshStructureToolBar(title, toolbarKey, arguments, unrefreshedNodeId, changeTypeId, newNodeId)
{
	if(parent.frames["toolbar"].document)
	{
		//alert('toolbarKey:' + toolbarKey);
		//toolbarKey 	= escape(toolbarKey);	
		toolbarKey 	= hexcode(toolbarKey);	
		if(parent.frames["toolbar"].document.getElementById("lockLayer"))
			parent.frames["toolbar"].document.getElementById("lockLayer").style.display="block";
		
		if(cmsContext != "")
			parent.frames["toolbar"].location.href = cmsContext + '/ViewStructureToolToolBar.action?title=' + title + '&toolbarKey=' + toolbarKey + '&' + arguments;
		else
			parent.frames["toolbar"].location.href = 'ViewStructureToolToolBar.action?title=' + title + '&toolbarKey=' + toolbarKey + '&' + arguments;
		
		//alert("unrefreshedNodeId:" + unrefreshedNodeId);
		if(unrefreshedNodeId > 0)
		{
			//alert("About to call refresh on applet...");
			parent.frames["menu"].refreshNode(unrefreshedNodeId, changeTypeId, newNodeId);
		}
	}
}

//-----------------------------------------------
// This function reloads the toolbar above the current 
// active action so that buttons related to the view 
// can be shown and a suitable headline be written.
//-----------------------------------------------

function refreshPublishingToolBar(title, toolbarKey, arguments)
{
	//alert('toolbarKey:' + toolbarKey);
	//toolbarKey 	= escape(toolbarKey);	
	toolbarKey 	= hexcode(toolbarKey);	
	parent.frames["toolbar"].location.href = 'ViewPublishingToolToolBar.action?title=' + title + '&toolbarKey=' + toolbarKey + '&' + arguments;
}

//-----------------------------------------------
// This function reloads the toolbar above the current 
// active action so that buttons related to the view 
// can be shown and a suitable headline be written.
//-----------------------------------------------

function refreshMyDesktopToolBar(title, toolbarKey, arguments)
{
	//alert('toolbarKey:' + toolbarKey);
	//toolbarKey 	= escape(toolbarKey);	
	toolbarKey 	= hexcode(toolbarKey);	
	parent.frames["toolbar"].location.href = 'ViewMyDesktopToolToolBar.action?title=' + title + '&toolbarKey=' + toolbarKey + '&' + arguments;
}

//-----------------------------------------------
// This function sets a different command to the editForm so that  
// the user can be handled different after the submit
//-----------------------------------------------

function saveAndExit(form, action)
{
	showWorking();
	//alert('action:' + action);
	form.action = action;
	form.submit();
}



//-----------------------------------------------
// This function sets a different command to the editForm so that  
// the user can be handled different after the submit
//-----------------------------------------------

function changeSiteNodeState(siteNodeVersionId)
{
	stateId    	  = document.editForm.stateId.value;
	siteNodeId    = document.editForm.siteNodeId.value;
	repositoryId  = document.editForm.repositoryId.value;
	//alert("stateId:" + stateId);
	//alert("siteNodeId:" + siteNodeId);
	//alert("repositoryId:" + repositoryId);
	
	if (confirm('Are you sure you want to change state? If you have any unsaved changes they will be lost.'))
	{
		//if(stateId == 2) //If publish
		//	window.location.href="ViewSiteNode!commentVersion.action?siteNodeVersionId=" + siteNodeVersionId + "&stateId=" + stateId + "&siteNodeId=" + siteNodeId + "&repositoryId=" + repositoryId;
		//else
			window.location.href="ChangeSiteNodeState.action?siteNodeVersionId=" + siteNodeVersionId + "&stateId=" + stateId + "&siteNodeId=" + siteNodeId + "&repositoryId=" + repositoryId;
	}
}


//-----------------------------------------------
// This function sets the focus on a given field
//----------------------------------------------- 
function focus(formField)
{
	if(formField)
		formField.focus();
}			

//-----------------------------------------------
// This function opens up a new location in a 
// restriced popup 
//-----------------------------------------------
function openPopup(url, name, details)
{
	newWin=window.open(url, name, details);
	newWin.focus();
}

//-----------------------------------------------
// This function opens up a new location in a 
// restriced popup 
//-----------------------------------------------
function openDatePopup(event, url, name)
{
	setDirty(); // TODO: Assume that change will be made, improve later
	
	details = 'width=200,height=220,left=' + getEventFramePositionX(event) + ',top=' + getEventFramePositionY(event) + '';
	
	newWin=window.open(url, name, details);
	newWin.focus();
}

//-----------------------------------------------
// This function opens up a new tool-window
//-----------------------------------------------
function openToolWindow()
{
	url = top.window.location;
	now = new Date();
	details = "toolbar=no,status=yes,scrollbars=no,location=no,menubar=no,directories=no,resizable=yes,width=1000,height=740,left=5,top=5";
	windowName = "CMS" + now.getSeconds();
	newWin=window.open(url, windowName, details);
	newWin.focus();
}

//-----------------------------------------------
// This function calls a url if confirmed  
//-----------------------------------------------

function confirmAction(text, url)
{
	if (confirm(text))
	{
		window.location.href = url;
	}
}

function setDirty()
{
	dirty=true;
}
function getDirty()
{
	return dirty;
}

function getEventFramePositionX(e) 
{
	if (navigator.appName == "Microsoft Internet Explorer")
	{
    	mX = event.clientX + 200;
  	}
  	else 
  	{
    	mX = e.pageX + 200;
  	}
  	
  	return mX;
}

function getEventFramePositionY(e) 
{
	if (navigator.appName == "Microsoft Internet Explorer")
	{
    	mY = event.clientY + 200;
  	}
  	else 
  	{
    	mY = e.pageY + 200;
  	}
  	
  	return mY;
}

//-----------------------------------------------
// These functions are here to properly encode a string in javascript
//-----------------------------------------------

function hexnib(d) {
  if(d<10) return d; else return String.fromCharCode(65+d-10);
}

function hexbyte(d) {
       return "%"+hexnib((d&240)>>4)+""+hexnib(d&15);
}

function hexcode(url) {
    var result="";
   var hex="";
    for(var i=0;i<url.length; i++) {
            var cc=url.charCodeAt(i);
            if (cc<128) {
                result+=hexbyte(cc);
            } else if((cc>127) && (cc<2048)) {
               result+=  hexbyte((cc>>6)|192)
                       + hexbyte((cc&63)|128);
            } else {
               result+=  hexbyte((cc>>12)|224)
                       + hexbyte(((cc>>6)&63)|128)
                       + hexbyte((cc&63)|128);
            }
    }
   return result;
}


/**
 * This method close layer.
 */
 
function closeDiv(id)
{
	document.getElementById(id).style.display = 'none';
}

function closeChat()
{
	closeDiv('systemMessagesDialog'); 
	closeDiv('systemMessages');
	document.getElementById("chatIFrame").src = "";
}
