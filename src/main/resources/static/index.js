console.log('hi');

var socket = new WebSocket(`ws://${ location.host }/socket`);

socket.onmessage = function (event) {
  console.log(event.target.data);
};

var interval = setInterval(function() {
  socket.send(JSON.stringify({ name: 'toast' }));
}, 1000);

socket.onerror = function(err) {
  console.error(err);

  clearInterval(interval);
};
