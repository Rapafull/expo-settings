import { StatusBar } from "expo-status-bar";
import React from "react";
import { Button, NativeModules, StyleSheet, Text, View } from "react-native";


export default function App() {
  
  const [count, setCount] = React.useState(0);
  var [data, setLocation] = React.useState("0");

  const onPressLocation = async () => {
    setLocation(data="0")
   const result = await NativeModules.LocationModule.getLocation();
   console.log();
    setLocation(data=`Lat: ${result["latitude"]} Lon : ${result["longitude"]}`);
    console.log(data);
  };

  const onPressStartLocation = async () => {
    data = await NativeModules.LocationModule.startLocationTracker({
      minTimeMs: 6000,
      minDistanceM: 0,
    });
    console.log(data);
  };

  const stopLocation = async () => {
    data = await NativeModules.LocationModule.stopLocationTracker();
    console.log(data);
  };

  const checkPermission = async () => {
    data = await NativeModules.PermissionsModule.checkPermissions();
    console.log(data);
  };

  const getPermissions = async () => {
    data = await NativeModules.PermissionsModule.requestPermissions();
    console.log(data);
  };

  const changeColor=()=>{
    setCount(count+1);
  }
  return (
    <View style={styles.container}>
       <View style={styles.button}>
        <Button title="check permissions" onPress={checkPermission} />
      </View>
       <View style={styles.button}>
        <Button title="Get permissions" onPress={getPermissions} />
      </View>
      <View style={styles.button}>
        <Button title="Get location" onPress={onPressLocation} />
      </View>
      <View style={styles.button}>
        <Button title="Start location" onPress={onPressStartLocation} />
      </View>
      <View style={styles.button}>
        <Button title="Stop location" onPress={stopLocation} />
      </View>
      <View style={styles.button}>
        <Button color="black" title="Account" onPress={changeColor} />
      </View>
      <Text> Location {data} </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#FFFFFF",
    alignItems: "center",
    justifyContent: "center",
  },
  button: {
    marginBottom: 20,
    backgroundColor:"#000000"
  },
});
