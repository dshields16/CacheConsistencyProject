::the third number in brackets should be equal to the number of edge nodes simulated

for /l %%x in (0, 1, 2) do (

	::file path ending in NodeConsistencyControl should point to the corresponding file in the repo
	::after %%x the parameters are as follows:
	::
	::number of edge devices
	::boolean - use PATA optimisation
	::interval between cache updates, ms
	::number of messages to be sent, rate is determined by interval
	::seed value, may be left out for a random seed

	start cmd.exe /k "cd /d D:\...\CacheConsistencyProject\EdgeOptimisation\out\artifacts\NodeConsistencyControl & java -jar EdgeOptimisation.jar %%x 3 false 200 100 201"
	
)