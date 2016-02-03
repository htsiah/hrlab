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
    });
    
    // Bind leave type field 
    $("#lt").change(function() {
    	var selectedLT = $( "#lt option:selected" ).text();
		$.ajax({
			url: "/leavepolicy/getdaytypejson/" + selectedLT,
			contentType: "application/json; charset=utf-8",
			success: function(data){
				if (data.daytype == "Full day only") {
					$("#dt").attr("disabled", "disabled");
					$("#dt").val("Full day");
					$("#tdat").removeAttr("disabled");
				} else {
					$("#dt").removeAttr("disabled");
				}
			},
			error: function(xhr, status, error){
				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.")
			},
		});
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
	
});

// Form submit function
var handleSubmit = function() {
	$('#leaveform').submit();	
}