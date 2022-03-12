import os
path = os.curdir
change_tag = {'nuggets/copper': 'copper_nuggets',
              'plates/brass' : 'brass_plates',
              'plates/gold' : 'gold_plates',
              'plates/iron' : 'iron_plates',
              'plates/copper' : 'copper_plates'

              }
for (root, dirs, files) in os.walk(path):
    for file_name in files:
        if file_name.endswith('json'):
            filepath = os.path.join(root, file_name)
            with open(filepath, 'rt') as file:
                try:
                    x = file.read()
                except:
                    continue
                if not any((i in x) for i in change_tag):
                    continue
            with open(filepath, 'wt') as file:
                print(filepath)
                for tags in change_tag:
                    x = x.replace(tags, change_tag[tags])
                file.write(x)
        

