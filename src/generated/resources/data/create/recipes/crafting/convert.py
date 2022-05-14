import os
import json
writepath = './compat'
for a,b ,cs in os.walk('.'):
    for c in cs:
        fn = a + '\\'+ c
        if fn.find('json') == -1:
            continue
        flag = False
        with open(fn, 'r') as fnf:
            js = json.load(fnf)
            if 'ingredients' in js.keys():
                ing = js['ingredients']
                for i in ing:
                    if type(i) == dict and 'item' in i.keys() and i['item'] == 'create:brass_ingot':
                        ing.remove(i)
                        ing.append({'tag' : 'c:brass_ingots'})
                        flag = True
                    if type(i) == dict and 'tag' in i.keys() and i['tag'] == "c:ingots/brass":
                        ing.remove(i)
                        ing.append({'tag' : 'c:brass_ingots'})
                        flag = True
            if 'key' in js.keys():
                for key in js['key'].keys():
                    if 'tag' in js['key'][key].keys() and js['key'][key]['tag'] == "c:ingots/brass":
                        js['key'][key]['tag'] = "c:brass_ingots"
                        flag = True
                    elif 'item' in js['key'][key].keys() and js['key'][key]['item'] == "create:brass_ingot":
                        js['key'][key]['tag'] = "c:brass_ingots"
                        flag = True
                        
            if flag:
                with open(writepath + '\\'+ c, 'w') as new:
                    json.dump(js, new)
                
