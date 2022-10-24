vertx.eventBus().consumer("hello.named.addr").handler({
    msg.reply("Hello ${msg.body()}!")
})
