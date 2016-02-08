$(document).ready(function() 
		{
	$("#createClaimBtn").click(function() 
			{
		$('input[name="file"]').each(function(index, value) 
				{ 
			var file = value.files[0];
			if(file) 
			{
				var formData = new FormData();
				formData.append('file', file);
				$.ajax({
					url : 'rest/claim/create',
					type : 'POST',
					data : formData,
					cache : false,
					contentType : false,
					processData : false,
					success : function(data, textStatus, jqXHR) {
						var message = jqXHR.responseText;
						if("Duplicate"== message){
							var response = confirm("Duplicate record found. Do you want to update the claim?");
							if(response){
								$.ajax({
									url : 'rest/claim/update',
									type : 'POST',
									cache : false,
									data : false,
									contentType : false,
									processData : false,
									success : function(data, textStatus, jqXHR) {
										var message = jqXHR.responseText;
										$("#messages").html("<li>" + message + "</li>");
									},
									error : function(jqXHR, textStatus, errorThrown) {
										$("#messages").html("<li style='color: red;'>" + textStatus  + "</li>");
									}
								});                         	   
							}
							else{
								$.ajax({
									url : 'rest/claim/deleteMCTObject',
									type : 'POST',
									cache : false,
									data : false,
									contentType : false,
									processData : false,
									success : function(data, textStatus, jqXHR) {
										$("#messages").html("<li>" + "Claim Not updated" + "</li>");
									},
									error : function(jqXHR, textStatus, errorThrown) {
										$("#messages").html("<li style='color: red;'>" + textStatus + "</li>");
									}
								});           
							}              
						}
						else{                               	
							$("#messages").html("<li>" + message + "</li>");
						}
					},
					error : function(jqXHR, textStatus, errorThrown) {
						$("#messages").html("<li style='color: red;'>" + textStatus + ": Invalid XML Schema </li>");
					}
				});
			}
			else{                   	
				alert("Please select a claim to upload");
			}
				});
			});

	$("#submit_read").click(function() 
			{        
		var input_read = document.getElementById('input_read').value;
		var read_value = document.getElementById('read_value').value;
		if(input_read == "" || read_value ==""){
			alert("Please select a valid input");
		}
		else{
			$.ajax({
				url : "rest/claim/read/"+input_read+"?value="+read_value,
				type : 'GET',
				data : false,
				cache : false,
				contentType : false,
				processData : false,
				success : function(data, textStatus, jqXHR) {
					var message = jqXHR.responseText;
					$("#read_message").html("<li>" + message + "</li>");
				},
				error : function(jqXHR, textStatus, errorThrown) {
					$("#read_message").html("<li style='color: red;'>" + textStatus + "</li>");
				}
			}); 
		}
			}); 
		});

