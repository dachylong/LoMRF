#!/bin/bash


lomrf -infer marginal -i dec7a_ext.mln -e fra1gt_evidence.db -r lomrf-output-decomposed.result \
	-q HoldsAt/2 -owa InitiatedAt/2,TerminatedAt/2 \
	-cwa Happens/2,Close/4,OrientationMove/3,StartTime/1,Next/2 $@
