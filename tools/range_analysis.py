import math

def estimate(soc, consumption, usable=0.9):
    if not all(map(math.isfinite,(soc,consumption,usable))) or not 0<=soc<=100 or consumption<=0 or not 0<usable<=1:
        raise ValueError('Invalid inputs')
    return 4000*usable*max(0,soc-10)/100/consumption

if __name__=='__main__':
    assert abs(estimate(75,35)-66.857142857)<1e-6
    assert estimate(5,35)==0
    print('Range checks passed')
