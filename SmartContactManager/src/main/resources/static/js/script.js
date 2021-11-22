console.log("this is script file.")

function toggleSidebar(){
	if($(".sidebar").is(":visible")){
		$(".sidebar").css("display","none");
		$(".content").css("margin-left","0%");	
	}else{
		$(".sidebar").css("display","block");
		$(".content").css("margin-left","20%");	
	}
}

const search=()=>{
	let query=$("#search-input").val();
	
	if(query=='')
		$(".search-result").hide();		
	else{	
	//sending request to server
	let url=`http://localhost:8080/search/${query}`;
	
	fetch(url)
	.then(response=>{
		return response.json();
	}).then((data)=> {
		
		let text=`<div class='list-group'>`
		
		data.forEach(contact => {
			text+=`<a href='/user/${contact.cId}/contact' class='list-group-item list-group-item-action'>
				${contact.name}
			</a>`
		});
		
		text+=`</div>`
		
		$(".search-result").html(text);
		$(".search-result").show();	
	});
	}
};

//first request to create order

const paymentStart=()=>{
	console.log("Payment Started...");
	let amount=$("#payment_field").val();
	console.log(amount);
	if(amount=='' || amount==null){
		//alert("Amount is Required !!");
		swal("Failed !!","Amount is Required!!","error");
		return;
	}

	//code..
	//we will use ajax to send request to razorpay server to create order
	
	$.ajax({
		url:'/user/create-order',
		data:JSON.stringify({amount:amount,info:'order_request'}),
		contentType:'application/json',
		type:'Post',
		dataType:'json',
		success:function(response){
			//invoked when success
			console.log(response);

			if(response.status=='created'){
				//open payment form
				let options={
					key:'rzp_test_mWuBoSt0xmHBbJ',
					amount:response.amount,
					currency:'INR',
					name:'Smart Contact Manager',
					description:'Donation',
					image:'data:image/jpeg;base64,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',
					order_id:response.id,
					handler:function(response){
						console.log(response.razorpay_payment_id);
						console.log(response.razorpay_order_id);
						console.log(response.razorpay_signature);
						console.log('Payment Successful !!');
						//alert("Congratzz !! Payment Successful");

						updatePaymentOnServer(
						response.razorpay_payment_id,
						response.razorpay_order_id,
						"paid"
						);
					},
					"prefill": {
					"name": "",
					"email": "",
					"contact": ""
					},
					"notes": {
					"address": "Sam Home"

					},
					"theme": {
					"color": "#3399cc"
					},
				};

				let rzp=new Razorpay(options);
				
				rzp.on('payment.failed', function (response){
					console(response.error.code);
					console(response.error.description);
					console(response.error.source);
					console(response.error.step);
					console(response.error.reason);
					console(response.error.metadata.order_id);
					console(response.error.metadata.payment_id);
					//alert("Oops Payment Failed !!")
					swal("Failed !!","Oops payment failed !!","error");
				});

				rzp.open();
				
			}
		},
		error:function(error){
			//invoked when error
			console.log(error);
			alert('Something Went Wrong !!');
		}
	});
};

function updatePaymentOnServer(payment_id,order_id,status){
	$.ajax({
		url:'/user/update-order',
		data:JSON.stringify({
			payment_id:payment_id,
			order_id:order_id,
			status:status
		}),
		contentType:'application/json',
		type:'Post',
		dataType:'json',
		success:function(response){
			swal("Good job!","Congrats !! Payment Successful !!","success");
		},
		error:function(error){
			swal("Failed !!",
			"Your payment is Successful, but we did not get on server, we will contact ASAP!!",
			"error");
		}
	});
};