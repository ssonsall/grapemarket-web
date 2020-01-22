<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="sec"
	uri="http://www.springframework.org/security/tags"%>
<!doctype html>
<html lang="en">
<head>
<title>Websocket ChatRoom</title>
<!-- Required meta tags -->
<meta charset="utf-8">
<meta name="viewport"
	content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<script src="https://cdn.jsdelivr.net/npm/vue/dist/vue.js"></script>
<script src="https://unpkg.com/axios/dist/axios.min.js"></script>
<script
	src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.4.0/sockjs.min.js"></script>
<link
	href="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css"
	rel="stylesheet"
	integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh"
	crossorigin="anonymous">
<script
	src="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js"
	integrity="sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6"
	crossorigin="anonymous"></script>
<script
	src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>



<!-- Bootstrap CSS -->
<!-- <link rel="stylesheet" href="/webjars/bootstrap/4.3.1/dist/css/bootstrap.min.css"> -->
<style>
[v-cloak] {
	display: none;
}

div.container2 {
	overflow-x: auto;
	overflow-y: scroll;
	display: inline-block;
	border: solid 1px green;
	height: 400px;
	width: 380px;
}
</style>
</head>
<body>
	<sec:authorize access="isAuthenticated()">
		<sec:authentication property="principal" var="principal" />
	</sec:authorize>
	<input type="hidden" id="sendername" value="${principal.user.username}" />
	<div class="container" id="app" v-cloak>


		<div class="container2" id="container2">

			<ul class="list-group">

				<c:forEach var="message" items="${messages}">
					<li class="list-group-item">${message.sender}-
						${message.message}</a>
					</li>
				</c:forEach>
				<li class="list-group-item" v-for="message in messages">
					{{message.sender}} - {{message.message}}</a>
				</li>
			</ul>
		</div>


		<div class="input-group">
			<div class="input-group-prepend">

				<label class="input-group-text">내용</label>
			</div>
			<input type="text" id="message" name="message" class="form-control"
				v-model="message" @keyup.enter="sendMessage">
			<div class="input-group-append">
				<button class="btn btn-primary" type="button" @click="sendMessage">보내기</button>
			</div>
		</div>
	</div>
	<!-- JavaScript -->
	<!--     <script src="/webjars/vue/2.5.16/dist/vue.min.js"></script>
    <script src="/webjars/axios/0.17.1/dist/axios.min.js"></script>
    <script src="/webjars/bootstrap/4.3.1/dist/js/bootstrap.min.js"></script>
    <script src="/webjars/sockjs-client/1.1.2/sockjs.min.js"></script>
    <script src="/webjars/stomp-websocket/2.3.3-1/stomp.min.js"></script> -->
	<script>
        // websocket & stomp initialize
        var sendername = document.getElementById('sendername').value;
        var sock = new SockJS("/ws-stomp");
        var ws = Stomp.over(sock);
        // vue.js
        var vm = new Vue({
            el: '#app',
            data: {
                roomId: '',
                room: {},
                sender: '',
                message: '',
                messages: []
            },
            created() {
                this.roomId = localStorage.getItem('wschat.roomId');
                this.sender = sendername;
                this.message = '';
                this.findRoom();
            },
            methods: {
                findRoom: function() {
                    axios.get('/chat/room/'+this.roomId).then(response => { this.room = response.data; });
                },
                sendMessage: function() {
                    ws.send("/pub/chat/message", {}, JSON.stringify({temp:this.roomId, sender:this.sender, message:this.message}));
                    this.message = '';
                },
                recvMessage: function(recv) {
                    this.messages.push({"type":recv.type,"sender":recv.type=='ENTER'?'[알림]':recv.sender,"message":recv.message})
                }
            }
        });
        // pub/sub event
        ws.connect({}, function(frame) {
            ws.subscribe("/sub/chat/room/"+vm.$data.roomId, function(message) {
                var recv = JSON.parse(message.body);
                vm.recvMessage(recv);
                
            });
        }, function(error) {
        });

        document.getElementById('container2').scrollTop = document.getElementById('container2').scrollHeight 
    </script>
	<%@include file="../include/script.jsp"%>
</body>
</html>