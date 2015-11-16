#-*-coding:utf-8-*-
import web
import os
import socket
import time
from basic import *

urls = (
    '/(.*)', 'index'
)
app = web.application(urls, globals())

# initial global variable here
HOST = '192.168.1.2'
PORT = 6000
ADDR = (HOST,PORT)

moving = 'up'
fp = ''

def login(para):
    return para

def profile(para):
    return para

def power(para):
    return para

def lanedetecton(para):
    return para

def realtimeethernet(para):
    return para
def carcontrol(para):
    return para

def video(para):
    print para

# 用字典实现一个switch
operator = {
    'login':login,
    'profile':profile,
    'power':power,
    'lanedetecton':lanedetecton,
    'realtimeethernet':realtimeethernet,
    'carcontrol':carcontrol,
    'video':video,
}

class index:
    def __init__(self):
        self.render = web.template.render('get/')
    def GET(self, name):
        # distinguish different webbrowser
        # we can get more infomation from web.ctx
        #checkWebBrowser(web.ctx.env.get('HTTP_USER_AGENT'))
        return self.render.index()
    def POST(self, name):
        keys = name.split('/')
        key  = keys[len(keys)-1]
        if operator.has_key(key):
            operator.get(key)(web.input())
        else:
            return self.render.index()

                
class motor:
    def __init__(self):
        global fp
        global moving        # if not define global variable here, system will regard as a local one
        fp = open("/tmp/fifo", "w")

    def GET(self):
        return 'hello world'

    def POST(self):
        global fp
        global moving
        para = web.input()
        dx = para['dx']
        dy = para['dy']
        direction = para['direction']
        if direction == 'right':
                if moving == 'up':
                        fp.write('ru__')
                elif moving == 'down':
                        fp.write('ld__')
        elif direction == 'rightup':
                fp.write('ru__')
                moving = 'up'
        elif direction == 'rightdown':
                fp.write('ld__')
                moving = 'down'
        elif direction == 'left':
                if moving == 'up':
                        fp.write('lu__')
                elif moving == 'down':
                        fp.write('rd__')
        elif direction == 'leftup':
                fp.write('lu__')
                moving = 'up'
        elif direction == 'leftdown':
                fp.write('rd__')
                moving = 'down'
        elif direction == 'up':
                fp.write('_u__')
                moving = 'up'
        elif direction == 'down':
                fp.write('_d__')
                moving = 'down'

        print 'direction : ', direction

        #TCP connection
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect(ADDR)
            s.send('hi~~')
            s.close()
        except Exception, e:
            print 'error:', e

if __name__ == '__main__':
    app.run()
