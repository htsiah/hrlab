$(function(){

    $("#navProfile").addClass("active open");
    $("#navMyProfile").addClass("active");
	
	$('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
	
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
	
	$("#personform").validate({
		debug: true,
		onkeyup: false,
		rules: {
			"p.fn": "required",
			"p.ln": "required",
			"p.mgrid": "required",
			"p.g": "required",
			"p.ms": "required",
			"p.dpm": "required",
			"p.off": "required",
			"p.edat": {
				required: true,
				date: true
			}
		},
		messages: {
			"p.fn": "Please enter first name.",
			"p.ln": "Please enter last name.",
			"p.mgrid": "Please select manager.",
			"p.g": "Please select gender.",
			"p.ms": "Please select marital status.",
			"p.dpm": "Please select department.",
			"p.off": "Please select Office.",
			"p.edat": {
				required: "Please enter employment start date.",
				customDate: "Please enter a valid date format dd-mmm-yyyy."
			}
		},		 
		submitHandler: function(form) {
			form.submit();
		 }
	});
	
	$.validator.addMethod(
		"customDate", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d\d?-\w\w\w-\d\d\d\d/);
		}, 
		"Please enter a valid date format dd-mmm-yyyy."
	);
	
});

$("#rl_admin").click(function(){
	if(this.checked){
		$("#p_rl").val("Admin");
	}else{
		$("#p_rl").val("");
	}
});