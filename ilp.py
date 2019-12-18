import pandas as pd
from pulp import *

data = pd.read_csv('../data/family_data.csv', index_col='family_id')
lims = pd.read_csv('n.csv')
lims = lims["x"].values

sol = pd.read_csv('/tmp/lala.csv')

fam_day_choices = {i:[] for i in range(0,5000)}
    

cost_dict = {0:  [  0,  0],
             1:  [ 50,  0],
             2:  [ 50,  9],
             3:  [100,  9],
             4:  [200,  9],
             5:  [200, 18],
             6:  [300, 18],
             7:  [300, 36],
             8:  [400, 36],
             9:  [500, 36 + 199],
             10: [500, 36 + 398],
            }

def cost(choice, members):
    x = cost_dict[choice]
    return x[0] + members * x[1]

santa = LpProblem('santa')

fam_choices = []
day_assignments = []
for i in range(0, 100):
    day_assignments.append([])

obj = []
#choices = ['choice_0', 'choice_1', 'choice_2', 'choice_3', 'choice_4', 'choice_5', 'choice_6', 'choice_7', 'choice_8', 'choice_9']
choices = ['choice_0', 'choice_1', 'choice_2', 'choice_3']
        
for j in range(0, 5000):
    sol_choice = sol['assigned_day'][j]
    fam_choice = []
    for i in range(0, len(choices)):
        choice = choices[i]
        k = data[choice][j]
        fam_day_choices[j].append(k)
        num_ppl = data['n_people'][j]
        penalty = cost(i, num_ppl)
        ass = LpVariable('ASS_{}_{}_{}'.format(i, j, k), lowBound=0, upBound=1, cat=LpBinary)
        if k == sol_choice:
            ass.setInitialValue(True)
        else:
            ass.setInitialValue(False)
        
        obj.append(penalty * ass)
        fam_choice.append(ass)
        day_assignments[k - 1].append(num_ppl * ass)

    fam_choices.append(fam_choice)

# minimise ass*pen +...+ ass*pen... 
santa += lpSum(obj)

# constraints

# family i must be assigned to one day 
# ASS_0_i_j + ASS_1_i_j + ASS_2_i_j == 1
for i in range(0, 5000):
    fam_choice = fam_choices[i]
    santa += lpSum(fam_choice) == 1, "AT_MOST_ONE_{}".format(i)

# each day must have between 125 and 300 ppl. 
for i in range(0, 100):
    day_assignment = day_assignments[i]
    #santa += lpSum(day_assignment) <= 300, "DAY_CONSTRAINT_LT{}".format(i+1)
    santa += lpSum(day_assignment) <= lims[i], "DAY_CONSTRAINT_LT{}".format(i+1)
    santa += lpSum(day_assignment) >= 125, "DAY_CONSTRAINT_GT{}".format(i+1)

santa.writeLP('santa.lp')

# using coin-or solver
threads = 12 
santa.solve(solver=COIN_CMD(msg=1, mip=1, presolve=1, strong=0, cuts=1, maxSeconds=60*300, dual=0, threads=threads))
# gnu
#santa.solve(solver=GLPK_CMD(keepFiles=0, mip=1, msg=1))

all_cap = 0
for i in range(0, 100):
    cap = lpSum(day_assignments[i]).value()
    all_cap += cap
    print(cap)


print("objective = ", value(santa.objective))

with open('lala.csv', 'w') as f:
    f.write("family_id,assigned_day\n")
    for i in range(0,5000):
        x = fam_choices[i]
        ass_v = [choice.value() for choice in x]
        ass = fam_day_choices[i][ass_v.index(max(ass_v))]
        print(ass_v)
        f.write("{},{}\n".format(i,ass))


