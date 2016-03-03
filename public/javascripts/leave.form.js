$(function(){
    
    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    $('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
			
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
    
	// To date can not earlier than From date
	$("#tdat").datepicker("setStartDate", $("#fdat").val());
	
    // Disable empty date field after selecting current date
    // http://stackoverflow.com/questions/24981072/bootstrap-datepicker-empties-field-after-selecting-current-date
    $("#fdat, #tdat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });

    $("#tdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        }
        setApplyBtn(true);
    });
    
    $("#fdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        } else {
    		$("#tdat").datepicker("setStartDate", $(this).val());
    		$("#tdat").datepicker("update", $(this).val());
        }
        setApplyBtn(true);
    });
        
    // Bind leave type field 
    $("#lt").change(function() {
    	var selectedLT = $( "#lt option:selected" ).val();
    	if (selectedLT != "") {
    		$.ajax({
    			url: "/leavepolicy/getdaytype/" + selectedLT,
    			dataType: "json",
    			beforeSend: function(){
    				loader.on();
    			},
    			success: function(data){
    				if (data.daytype == "Full day only") {
    					$("#dt").attr("disabled", "disabled");
    					$("#dt").val("Full day");
    					$("#tdat").removeAttr("disabled");
    				} else {
    					$("#dt").removeAttr("disabled");
    				}
    				setApplyBtn(false);
    				loader.off();
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
    				loader.off();
    			},
    		});	
    	} else {
    		setApplyBtn(true);
    	};
    });
    
	// Bind date type field 
	$(document).on('change', '#dt', function(e) {
		var seldatetype = this.options[this.selectedIndex].value;
		if (seldatetype=="1st half" || seldatetype=="2nd half") {
			$("#tdat").attr("disabled", "disabled");
			$("#tdat").val($("#fdat").val());
			$("#tdat").datepicker("update", $("#fdat").val());
		} else {
			$("#tdat").removeAttr("disabled");
		}
		setApplyBtn(true);
	})
		
	$.validator.addMethod(
		"customDate", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d\d?-\w\w\w-\d\d\d\d/);
		}, 
		"Please enter a valid date format d-mmm-yyyy."
	);
	
	$.validator.addMethod(
		"checkDate",
		function(value,element){
			var fdat = new Date($("#fdat").val());
			var tdat = new Date($("#tdat").val());
			
			if (fdat>tdat) {
				return false;
			} else {
				return true
			};
		},
		"Date to should greater than date from."
	);
	
	// Validation for form
	$("#leaveform").validate({
		debug: false,
		onkeyup: false,
		rules: {
			lt: "required",
			fdat: {
				required: true,
				customDate: true
			},
			tdat: {
				required: true,
				customDate: true,
				checkDate: true
			}
		},
		messages: {
			lt: "Please select a leave type",
			fdat: {
				required: "Please enter the date from.",
				customDate: "Please enter a valid date format d-mmm-yyyy."
			},
			tdat: {
				required: "Please enter the date to.",
				customDate: "Please enter a valid date format d-mmm-yyyy.",
				checkDate: "Date to should greater than date from."
			}
		},
		submitHandler: function(form) {
			$("#dt").removeAttr("disabled");
		 	$("#tdat").removeAttr("disabled");
		 	form.submit();
		}
	});
	
	// Show calendar
	Calendar.initCalendar();
	Calendar.showCompanyHoliday();
	Calendar.showMyLeave();
	Calendar.showOtherLeave();
	
});

var Calendar = {
	companyholidaysource:{
		url: '/companyholiday/getcompanyholidayjson/n',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching company holiday!');
		},
		className: 'label-success'
	},
		
	myapprovedleavessource:{
		url: '/leave/getapprovedleavejson/my/n',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
		
	otherapprovedleavessource:{
		url: '/leave/getapprovedleavejson/allexceptmy/n',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
		
	initCalendar:function(){
		var date = new Date();
		var d = date.getDate();
		var m = date.getMonth();
		var y = date.getFullYear();
		$('#calendar').fullCalendar({
		     eventRender: function(event, element) {
		    	 $(element).tooltip({title: event.tip});
		     }
		});	
	},

	removeEvents:function(p_source){
		$('#calendar').fullCalendar( 'removeEventSource', p_source )
	},

	showCompanyHoliday:function(){
		$('#calendar').fullCalendar('addEventSource',this.companyholidaysource);
	},
		
	showMyLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.myapprovedleavessource);
	},
	
	showOtherLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.otherapprovedleavessource);
	}
};

function setApplyBtn(p_loader) {
	
	var selPerson = $("#pid").val();
	var selLT = $("#lt").val();
	var selDT = $("#dt").val();
	var selFDat = $("#fdat").val();
	var selTDat = $("#tdat").val();
	
	if (selPerson=="" || selLT=="" || selDT=="" || selFDat=="" || selTDat=="") {
		$("#btnApply").text("Apply for 0 day");
		$("#btnApply").attr("disabled", "disabled");
	} else {
		
		$.ajax({
			url: "/leave/getapplyday/" + selPerson + "/" + selLT + "/" + selDT + "/" + selFDat + "/" + selTDat,
			dataType: "json",
			beforeSend: function(){
				if (p_loader) { loader.on() };
			},
			success: function(data){
				if (data.a <= 0) {
					$("#btnApply").text("Apply for 0 day");
					$("#btnApply").attr("disabled", "disabled");
				} else if (data.b < 0) {
					$("#btnApply").html("Apply for " + data.a + " day(s) <br /> No enough leave balance");
					$("#btnApply").attr("disabled", "disabled");
				} else {
					$("#btnApply").html("Apply for " + data.a + " day(s) <br />" + data.b + " day(s) remaining balance");
					$("#btnApply").removeAttr("disabled");
				}
				if (p_loader) { loader.off() };
			},
			error: function(xhr, status, error){
				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
				if (p_loader) { loader.off() };
			}
		});	

	}
};

// Form submit function
var handleSubmit = function() {
	$('#leaveform').submit();	
}