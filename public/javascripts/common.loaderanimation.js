var loader = {
	
	on:function() {
		$('#loader-overlay').show()
		$('#loader-overlay-center').spin({color: '#ffffff'});
	},
		
	off: function() {
		$('#loader-overlay-center').spin(false);
		$('#loader-overlay').hide();
	}
	
}
