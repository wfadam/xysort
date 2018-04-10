# xysort
A 2-lines solution to XY sorting

# Example 1
```java
        }   // channel loop

        XYSort.load("RandomData/xy.txt");
        XYSort.sort();

    }   // body

    //  @Override
    public void initialize() {
    }
}
```

# Example 2
```java
                    XYSort.load("RandomData/xy.txt");
                    String[] wxyl = {
                        String.format("%C%C", (char) SS.dutInfo.DUTList[duts[d] - 1].lotInfo.waferNoUpper[channel - 1][chip - 1], (char) SS.dutInfo.DUTList[duts[d] - 1].lotInfo.waferNoLower[channel - 1]
                        String.format("%C%C", (char) SS.dutInfo.DUTList[duts[d] - 1].lotInfo.xLocUpper[channel - 1][chip - 1], (char) SS.dutInfo.DUTList[duts[d] - 1].lotInfo.xLocLower[channel - 1][chip
                        String.format("%C%C", (char) SS.dutInfo.DUTList[duts[d] - 1].lotInfo.yLocUpper[channel - 1][chip - 1], (char) SS.dutInfo.DUTList[duts[d] - 1].lotInfo.yLocLower[channel - 1][chip
                        new String(SS.dutInfo.DUTList[duts[d] - 1].lotInfo.lotId[channel - 1][chip - 1], 0, 9)
                    };
                    XYSort.sort(duts[d], wxyl);
                    
                }   // dut loop
            }   // chip loop
```
