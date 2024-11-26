import pandas as pd
import matplotlib.pyplot as plt

# Load the CSV file
df = pd.read_csv('test.csv')

# Convert StartTime to datetime
df['StartTime'] = pd.to_datetime(df['StartTime'], unit='ms')

# Set StartTime as the index
df.set_index('StartTime', inplace=True)

# Counting the number of POST requests in each 6 seconds
throughput = df[df['RequestType'] == 'POST'].resample('6000L').count()['RequestType']

# Create a new time index in seconds
time_intervals = (throughput.index - throughput.index[0]).total_seconds()

# Plotting
plt.figure(figsize=(12, 6))
plt.plot(time_intervals, throughput.values, marker='o', linestyle='-', color='b')
plt.title('Throughput Over Time (6-Second Intervals)')
plt.xlabel('Time (seconds)')
plt.ylabel('Throughput (Requests per 6 seconds)')
plt.xticks(rotation=45)
plt.xticks(ticks=range(0, int(time_intervals.max()) + 1, 6), 
           labels=[f"{i}s" for i in range(0, int(time_intervals.max()) + 1, 6)])
plt.grid()
plt.tight_layout()
plt.savefig('throughput_over_time_6s.png')  # Save the plot as a PNG file
plt.show()
