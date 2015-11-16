#-*-coding:utf-8-*-

import re

def checkWebBrowser(userAgent):
    print '------>', userAgent
    mobile_rules = r'googlebot-mobile|android|avantgo|blackberry|blazer|elaine|hiptop|kindle|midp|mmp|moblie|o2|opera|wap'
    moblie = re.compile(mobile_rules, re.IGNORECASE)
    if moblie.search(userAgent) != None:
        print '-----haha-----'
    else:
        print '-----ohoh-----'
    return True


