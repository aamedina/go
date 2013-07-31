var socket = new WebSocket('ws://localhost:8000/websocket');

$(document).ready(function () {  
  var alphabet = 'ABCDEFGHJKLMNOPQRST'.split('');
  function renderBoard(callback) {
    var row = 0;
    var column = 0;
    for(var row = 19; row >= 1; row--) {
      for(var column = 0; column <= 18; column++) {
        $('div#board').append('<div class=\"square" id=' + alphabet[column] + row + '></div>');
      }
    }
    callback();
  }
  renderBoard(function () {    
    socket.onopen = function () {
      socket.send(JSON.stringify({ event: 'new-game' }));
    }
  });
  // $('#black-score').text(1);
  // $('#white-score').text(1);
  $('#board .square').on('click', function (e) {
    socket.send(JSON.stringify({ position: $(this).attr('id'), color: 'black', event: 'move'}));
  });
  socket.onmessage = function (event) {
    var data = JSON.parse(event.data);
    console.log(event)
    switch(data.event) {
      case 'move':
      $('#' + data.move).css('background-color', data.player);
      var player = data.player === 'black' ? 'white' : 'black';
      socket.send(JSON.stringify({ event: 'move-callback', player: player }));
      break;
      case 'score':
      $('#black-score').text(data.black);
      $('#white-score').text(data.white);
      break;
    }
  };
});
