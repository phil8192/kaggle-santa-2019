import pandas as pd
#from pulp import *
#https://buildmedia.readthedocs.org/media/pdf/python-mip/latest/python-mip.pdf
from mip.model import *

data = pd.read_csv('../data/family_data.csv', index_col='family_id')
lims = pd.read_csv('n.csv')
lims = lims["x"].values

diffs = pd.read_csv('diffs.csv')
diffs = diffs["x"].values

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

#santa = LpProblem('santa')
santa = Model(sense=MINIMIZE, solver_name=CBC)

fam_choices = []
day_assignments = []
for i in range(0, 100):
    day_assignments.append([])

obj = []
choices = ['choice_0', 'choice_1', 'choice_2', 'choice_3', 'choice_4', 'choice_5', 'choice_6', 'choice_7', 'choice_8', 'choice_9']
#choices = ['choice_0', 'choice_1', 'choice_2', 'choice_3', 'choice_4', 'choice_5']
        
for j in range(0, 5000):
    sol_choice = sol['assigned_day'][j]
    fam_choice = []
    for i in range(0, len(choices)):
        choice = choices[i]
        k = data[choice][j]
        fam_day_choices[j].append(k)
        num_ppl = data['n_people'][j]
        penalty = cost(i, num_ppl)
        #ass = LpVariable('ASS_{}_{}_{}'.format(i, j, k), lowBound=0, upBound=1, cat=LpBinary)
        ass = santa.add_var(name="ASS_{}_{}_{}".format(i,j,k), lb=0, ub=1, var_type=BINARY)
    
#        if k == sol_choice:
#            ass.x = 1 
#        else:
#            ass.x = 0
        
        obj.append(penalty * ass)
        fam_choice.append(ass)
        day_assignments[k - 1].append(num_ppl * ass)

    fam_choices.append(fam_choice)

extras = []
for i in range(0, 100):
    extras.append(santa.add_var(name="EXT_{}".format(i), lb=0, ub=200, var_type=INTEGER))

# minimise ass*pen +...+ ass*pen... 
#santa += lpSum(obj+extras)
santa.objective = minimize(xsum(obj+extras))

# constraints

# family i must be assigned to one day 
# ASS_0_i_j + ASS_1_i_j + ASS_2_i_j == 1
for i in range(0, 5000):
    fam_choice = fam_choices[i]
    #santa += lpSum(fam_choice) == 1, "AT_MOST_ONE_{}".format(i)
    santa += xsum(fam_choice) == 1, "AT_MOST_ONE_{}".format(i)

# each day must have between 125 and 300 ppl. 
for i in range(0, 100):
    day_assignment = day_assignments[i]
    santa += xsum(day_assignment) <= 300, "DAY_CONSTRAINT_LT{}".format(i+1)

    #santa += lpSum(day_assignment) <= lims[i], "DAY_CONSTRAINT_LT{}".format(i+1)
    santa += xsum(day_assignment) >= 125, "DAY_CONSTRAINT_GT{}".format(i+1)
#    santa += lpSum(day_assignment) == lims[i], "DAY_CONSTRAINT_EQ{}".format(i+1)

for i in range(1, 100):
    day_now = day_assignments[i]
    day_pre = day_assignments[i-1]
    #santa += lpSum(day_now) - lpSum(day_pre) <= 23, "CHANGE_CONSTRAINT_A_{}".format(i+1)
    #santa += lpSum(day_pre) - lpSum(day_now) <= 23, "CHANGE_CONSTRAINT_B_{}".format(i+1)
    extra = extras[i-1]
    diff_max = diffs[i-1] + extra
    santa += xsum(day_now) - xsum(day_pre) <= diff_max, "CHANGE_CONSTRAINT_A_{}".format(i+1)
    santa += xsum(day_pre) - xsum(day_now) <= diff_max, "CHANGE_CONSTRAINT_B_{}".format(i+1)


santa.write('santa.lp')

# using coin-or solver
#threads = 12 
#santa.solve(solver=COIN_CMD(msg=1, mip=1, presolve=1, strong=0, cuts=1, maxSeconds=60*400, dual=0, threads=threads))
# gnu
#santa.solve(solver=GLPK_CMD(keepFiles=0, mip=1, msg=1))

santa.preprocess=1
santa.opt_tol=1e-6
santa.max_mip_gap=1e-4
santa.lp_method=1 # AUTO=0, BARRIER=3, DUAL=1, PRIMAL=2
santa.integer_tol=1e-6
santa.threads=12
santa.verbose=1
santa.optimize(max_seconds=10)


#print("objective = ", value(santa.objective))

with open('lala.csv', 'w') as f:
    f.write("family_id,assigned_day\n")
    for i in range(0,5000):
        x = fam_choices[i]
        ass_v = [choice.x for choice in x]
        ass = fam_day_choices[i][ass_v.index(max(ass_v))]
        print(ass_v)
        f.write("{},{}\n".format(i,ass))


