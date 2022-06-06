#!/bin/bash

docker run -it --rm --name rabbitmq  --hostname my-rabbit -p 5672:5672 -p 15672:15672 rabbitmq
