#ifndef TIMEKEEPER_H
#define TIMEKEEPER_H

#define DATA_POINTS_N 65
#define MILLI_INTERVAL 123
#define SHIFT_BITS 6
/* This is an array of times (in milliseconds) mapped to a 12-bit ADC */
const uint16_t _current_curve[DATA_POINTS_N] ={65535,6666,5551,4900,4438,4081,3789,3542,3328,3139,2970,2818,2679,2550,2432,2321,2218,2121,2029,1943,1861,1783,1708,1637,1569,1504,1441,1380,1322,1266,1212,1159,1109,1059,1012,965,920,876,834,792,751,712,673,636,599,563,528,493,460,427,394,363,332,301,271,242,213,185,157,129,103,76,50,25,0};

/**
 * Call this to get the time elapsed (ms) for a 0.1uF CusTARD capacitor (20MΩ discharge) on a 1.8V VCC
 * @param  _adc_14_value [description]
 * @return               [description]
 */
inline uint16_t fast_tardis(uint16_t _adc_14_value) 
{
	int16_t left = _adc_14_value >> SHIFT_BITS;
	return  _current_curve[left] -
				((
					(uint32_t)(_current_curve[left] - _current_curve[left+1]) * 
					(uint32_t)(_adc_14_value - (left << SHIFT_BITS))
				) >> SHIFT_BITS);
};

#endif // TIMEKEEPER_H