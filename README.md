# Java Thread Benchmark

This project demonstrates a performance comparison between platform threads and virtual threads in Java, using a simple HTTP server and client setup.

## Overview

The benchmark creates an HTTP server that performs a CPU-intensive task for each request. It then sends a large number of concurrent requests to this server and measures the response times. The test is run twice: once using platform threads and once using virtual threads.

## Requirements

- Java 21 or later (for virtual thread support)
- Maven or Gradle (for dependency management)

## Dependencies

- Eclipse Jetty (for the HTTP server)
- Jakarta Servlet API

## How it works

1. The `Main` class sets up and runs the benchmark:
    - It creates a Jetty server with a `CPUIntensiveServlet`.
    - It sends a large number of concurrent HTTP requests to this server.
    - It measures and reports on the response times.

2. The benchmark is run twice:
    - Once with platform threads
    - Once with virtual threads

3. The `CPUIntensiveServlet` simulates a CPU-intensive task by running a loop for 1 second.

4. The client sends requests using `HttpClient`, utilizing either a fixed thread pool (for platform threads) or a virtual thread per task executor (for virtual threads).

## Configuration

You can adjust the following constants in the `Main` class:

- `PORT`: The port on which the server listens (default: 8080)
- `CONCURRENT_REQUESTS`: The number of concurrent requests (default: 1000)
- `TOTAL_REQUESTS`: The total number of requests to send (default: 10000)

## Running the benchmark

To run the benchmark, simply execute the `main` method in the `Main` class. The program will:

1. Run the benchmark with platform threads
2. Run the benchmark with virtual threads
3. Print statistics for both runs, including:
    - Mean response time
    - Standard deviation of response times
    - Maximum response time

## Results interpretation

The results will show you the performance difference between platform threads and virtual threads for this specific CPU-bound task. Generally, you might expect:

- Similar mean response times for both thread types, as the task is CPU-bound
- Potentially lower standard deviation and maximum response time for virtual threads, due to reduced scheduling overhead

Remember that the benefits of virtual threads are more pronounced in I/O-bound tasks, so this CPU-bound example may not show dramatic differences.

## Notes

- This benchmark is designed to stress-test the system. Ensure you have adequate system resources available when running it.
- The server has a 2-second startup delay to ensure it's fully ready before the client starts sending requests.
- Error handling is included to catch and report connection failures, which might occur if the server becomes overloaded.

## License

[Specify your chosen license here]

## Contributing

[Add your guidelines for contributing to this project]